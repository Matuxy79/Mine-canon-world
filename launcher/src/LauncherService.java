import com.google.gson.*;
import java.io.*;
import java.nio.channels.*;
import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;
import java.awt.Desktop;
import java.net.URI;

final class LauncherService {
    static final String VERSION="1.20.1-forge-47.4.26";
    static final String INSTALLER="forge-1.20.1-47.4.26-installer.jar";
    static final String SHA="138961bc2a5f085ced0b5db2cd72c6caad20b25e22bcdccd031b4fc9182e8186";
    static final String[] IDS={"first-breath","nocturne-west","chakra-nations","nen-new-world","curse-modernity","dark-continent"};
    static final String[] NAMES={"First Breath","Nocturne West","Chakra Nations","Nen New World","Curse Modernity","Dark Continent"};
    static final String[] LORE={"Breath, bending and the first disciplines of the Canon Field.","Bloodlines, sacred relics and the moonlit western kingdoms.","Seals, summons and the rise of the clan-state civilizations.","Aura, contracts and the age of licensed hunters.","Domains, binding vows and the hidden urban world.","Forbidden archaeology beyond the boundaries of known canon."};
    final Path home, bundle;
    final Properties config=new Properties();
    LauncherService(Path home,Path bundle)throws IOException {
        this.home=home;this.bundle=bundle;
        if(Files.isRegularFile(home.resolve("launcher.properties")))try(var in=Files.newInputStream(home.resolve("launcher.properties"))){config.load(in);}
    }
    static Path defaultHome(){return Path.of(System.getProperty("minecanon.home",Path.of(System.getProperty("user.home"),"MineCanon").toString()));}
    static Path defaultMinecraft(){
        String os=System.getProperty("os.name").toLowerCase();
        if(os.contains("win")){String a=System.getenv("APPDATA");if(a!=null)return Path.of(a,".minecraft");}
        if(os.contains("mac"))return Path.of(System.getProperty("user.home"),"Library","Application Support","minecraft");
        return Path.of(System.getProperty("user.home"),".minecraft");
    }
    Path minecraft(){return Path.of(config.getProperty("minecraft",defaultMinecraft().toString()));}
    int selected(){try{return Math.max(0,Math.min(IDS.length-1,Integer.parseInt(config.getProperty("selected","0"))));}catch(Exception e){return 0;}}
    int memory(){try{return Math.max(2,Math.min(16,Integer.parseInt(config.getProperty("ram","4"))));}catch(Exception e){return 4;}}
    Path instance(int i){return home.resolve("instances").resolve(IDS[i]);}
    void save()throws IOException {Files.createDirectories(home);Path temp=Files.createTempFile(home,"config-",".tmp");try{try(var out=Files.newOutputStream(temp)){config.store(out,"Mine Canon - local launcher settings");}atomic(temp,home.resolve("launcher.properties"));}finally{Files.deleteIfExists(temp);}}
    void select(int i)throws IOException {if(i<0||i>=IDS.length)throw new IllegalArgumentException("Unknown age");config.setProperty("selected",Integer.toString(i));save();}
    boolean forgeInstalled(){return validVersion(minecraft().resolve("versions").resolve(VERSION).resolve(VERSION+".json"),VERSION);}
    boolean vanillaInstalled(){return validVersion(minecraft().resolve("versions/1.20.1/1.20.1.json"),"1.20.1");}
    static boolean validVersion(Path path,String id){try{var o=JsonParser.parseString(Files.readString(path)).getAsJsonObject();return o.has("id")&&id.equals(o.get("id").getAsString());}catch(Exception e){return false;}}
    String status(){return forgeInstalled()?"FORGE PROFILE DETECTED":"FORGE SETUP REQUIRED";}
    Path ensureInstance(int i)throws IOException {Path p=instance(i);Files.createDirectories(p.resolve("mods"));Files.createDirectories(p.resolve("saves"));Files.createDirectories(p.resolve("resourcepacks"));return p;}
    java.util.List<Path> mods(int i)throws IOException {Path p=instance(i).resolve("mods");if(!Files.isDirectory(p))return java.util.List.of();try(var s=Files.list(p)){return s.filter(x->Files.isRegularFile(x)&&x.getFileName().toString().toLowerCase().endsWith(".jar")).sorted().toList();}}
    Path profilesPath(){
        Path standard=minecraft().resolve("launcher_profiles.json"),store=minecraft().resolve("launcher_profiles_microsoft_store.json");
        String choice=config.getProperty("profiles","auto");
        if(choice.equals("store"))return store;if(choice.equals("standard"))return standard;
        if(Files.exists(standard)&&Files.exists(store))throw new IllegalStateException("Both launcher profile files exist. Choose Standard or Microsoft Store in Settings.");
        return Files.exists(store)?store:standard;
    }
    Path prepareProfile(int i)throws Exception {
        if(!forgeInstalled())throw new IOException("Install Forge 47.4.26 into the Minecraft folder selected in Settings first.");
        Path path=profilesPath();
        if(!Files.isRegularFile(path))throw new IOException("Minecraft launcher profile file not found. Open the official launcher once, then verify the Minecraft folder in Settings.");
        Path lockPath=minecraft().resolve("minecanon-profile.lock");
        try(var channel=FileChannel.open(lockPath,StandardOpenOption.CREATE,StandardOpenOption.WRITE);var lock=channel.tryLock()){
            if(lock==null)throw new IOException("Another Mine Canon window is updating profiles. Try again.");
            byte[] before=Files.readAllBytes(path);
            JsonObject root=JsonParser.parseString(new String(before,java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
            JsonObject profiles;
            if(!root.has("profiles")){profiles=new JsonObject();root.add("profiles",profiles);}else profiles=root.getAsJsonObject("profiles");
            String key="mine-canon-"+IDS[i];
            JsonObject p=profiles.has(key)?profiles.getAsJsonObject(key):new JsonObject();
            if(!p.has("created"))p.addProperty("created",Instant.now().toString());
            p.addProperty("name","Mine Canon | "+NAMES[i]);p.addProperty("type","custom");p.addProperty("lastVersionId",VERSION);
            p.addProperty("gameDir",ensureInstance(i).toAbsolutePath().toString());p.addProperty("javaArgs","-Xmx"+memory()+"G -Xms1G");
            p.addProperty("icon","Grass");profiles.add(key,p);
            Path backups=home.resolve("profile-backups");Files.createDirectories(backups);
            Path backup=backups.resolve(path.getFileName()+"."+System.currentTimeMillis()+"."+UUID.randomUUID()+".bak");
            Files.write(backup,before,StandardOpenOption.CREATE_NEW);
            Path tmp=Files.createTempFile(minecraft(),"mine-canon-",".json.tmp");
            try{
                Files.writeString(tmp,new GsonBuilder().setPrettyPrinting().create().toJson(root));
                if(!Arrays.equals(before,Files.readAllBytes(path)))throw new IOException("Launcher profiles changed while saving. Close the official launcher and try again.");
                atomic(tmp,path);
            }finally{Files.deleteIfExists(tmp);}
            return backup;
        }
    }
    static void atomic(Path from,Path to)throws IOException {try{Files.move(from,to,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(from,to,StandardCopyOption.REPLACE_EXISTING);}}
    Path javaExe(){return Path.of(System.getProperty("java.home"),"bin",System.getProperty("os.name").toLowerCase().contains("win")?"java.exe":"java");}
    Path verifyInstaller()throws Exception {
        Path path=bundle.resolve(INSTALLER);if(!Files.isRegularFile(path))throw new IOException("Forge installer missing from the MineCanon app folder. Rebuild with Build-MineCanon.cmd.");
        String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
        if(!hash.equals(SHA))throw new IOException("The Forge installer differs from the pinned 47.4.26 file. Rebuild with Build-MineCanon.cmd.");return path;
    }
    Process startInstaller()throws Exception {
        Path path=verifyInstaller();Files.createDirectories(home);
        return new ProcessBuilder(javaExe().toString(),"-jar",path.toString()).directory(bundle.toFile()).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.appendTo(home.resolve("forge-setup.log").toFile())).start();
    }
    void openFolder(Path path)throws Exception {Files.createDirectories(path);if(!Desktop.isDesktopSupported()||!Desktop.getDesktop().isSupported(Desktop.Action.OPEN))throw new IOException("Open this folder manually: "+path);Desktop.getDesktop().open(path.toFile());}
    void openMinecraft()throws Exception {
        String custom=config.getProperty("launcher","").trim();
        if(!custom.isEmpty()){Path exe=Path.of(custom);if(!Files.isRegularFile(exe))throw new IOException("Minecraft launcher executable not found. Update Settings.");new ProcessBuilder(exe.toString()).start();return;}
        String os=System.getProperty("os.name").toLowerCase();
        if(os.contains("win")){
            for(String env:new String[]{"ProgramFiles(x86)","ProgramFiles"}){String base=System.getenv(env);if(base!=null){Path exe=Path.of(base,"Minecraft Launcher","MinecraftLauncher.exe");if(Files.isRegularFile(exe)){new ProcessBuilder(exe.toString()).start();return;}}}
            String script="$app = Get-StartApps | Where-Object { $_.AppID -like 'Microsoft.4297127D64EC6_*!*' -or $_.Name -eq 'Minecraft Launcher' } | Select-Object -First 1; if (!$app) { exit 2 }; Start-Process explorer.exe -ArgumentList ('shell:AppsFolder\\' + $app.AppID)";
            Process process=new ProcessBuilder("powershell.exe","-NoProfile","-NonInteractive","-Command",script).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
            if(!process.waitFor(10,java.util.concurrent.TimeUnit.SECONDS)){process.destroy();throw new IOException("Launcher lookup timed out. Set its executable in Settings.");}
            if(process.exitValue()!=0)throw new IOException("Minecraft Launcher was not found. Set its executable in Settings.");
        }else if(os.contains("mac")){new ProcessBuilder("open","-a","Minecraft").start();}
        else {new ProcessBuilder("minecraft-launcher").start();}
    }
}
