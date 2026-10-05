import com.google.gson.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;

/** Builds a resource pack that repairs broken model JSON shipped inside an instance's mods, without touching the mod jars.
 *  Two defects are fixed: a bare model parent such as "scrolls_water_2" (resolves to minecraft:models/scrolls_water_2.json,
 *  which never exists, so the item renders as a missing-model cube) and element rotations outside Minecraft's allowed angles. */
final class ModFixer {
    static final String PACK_NAME="MineCanon-Fixes.zip";
    static final String PACK_ENTRY="file/"+PACK_NAME;
    private static final double[] ANGLES={-45,-22.5,0,22.5,45};
    private static final Gson GSON=new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private ModFixer(){}

    record Result(int parentsFixed,int rotationsFixed,int modelsPatched){boolean any(){return modelsPatched>0;}}

    /** Scans every enabled mod jar in {@code mods} and writes the fix pack to {@code pack} (deleted when nothing needs fixing). */
    static Result build(Path mods,Path pack)throws IOException {
        int parents=0,rotations=0;
        Map<String,String> patched=new TreeMap<>();
        if(Files.isDirectory(mods)){
            List<Path> jars;try(var s=Files.list(mods)){jars=s.filter(p->p.getFileName().toString().toLowerCase().endsWith(".jar")).sorted().toList();}
            for(Path jar:jars){
                try(ZipFile zip=new ZipFile(jar.toFile())){
                    Map<String,String> byNameInNamespace=new HashMap<>();
                    List<String> models=new ArrayList<>();
                    for(var en=zip.entries();en.hasMoreElements();){
                        String name=en.nextElement().getName();
                        if(name.startsWith("assets/")&&name.contains("/models/")&&name.endsWith(".json")){
                            models.add(name);
                            byNameInNamespace.putIfAbsent(namespace(name)+"/"+baseName(name),name);
                        }
                    }
                    for(String name:models){
                        JsonObject o;
                        try{var parsed=JsonParser.parseString(new String(zip.getInputStream(zip.getEntry(name)).readAllBytes(),StandardCharsets.UTF_8));if(!parsed.isJsonObject())continue;o=parsed.getAsJsonObject();}
                        catch(Exception notJson){continue;}
                        boolean changed=false;
                        String ns=namespace(name);
                        if(o.has("parent")&&o.get("parent").isJsonPrimitive()){
                            String parent=o.get("parent").getAsString();
                            if(brokenParent(parent)){
                                String sibling=byNameInNamespace.get(ns+"/"+baseName(parent));
                                if(sibling!=null&&!sibling.equals(name))o.addProperty("parent",ns+":"+sibling.substring(sibling.indexOf("/models/")+8,sibling.length()-5));
                                else if(!o.has("elements")&&hasLayer0(o))o.addProperty("parent","item/generated");
                                else o.remove("parent");
                                parents++;changed=true;
                            }
                        }
                        int r=fixRotations(o);
                        if(r>0){rotations+=r;changed=true;}
                        if(changed)patched.put(name,GSON.toJson(o));
                    }
                }catch(ZipException|EOFException unreadable){/* a corrupt jar is reported by the mod scanner, not here */}
            }
        }
        if(patched.isEmpty()){Files.deleteIfExists(pack);return new Result(0,0,0);}
        Files.createDirectories(pack.getParent());
        Path temp=Files.createTempFile(pack.getParent(),"fix-",".tmp");
        try{
            try(ZipOutputStream out=new ZipOutputStream(Files.newOutputStream(temp))){
                put(out,"pack.mcmeta","{\"pack\":{\"pack_format\":15,\"description\":\"Mine Canon model fixes \\u00b7 by John Matukutire\"}}");
                for(var e:patched.entrySet())put(out,e.getKey(),e.getValue());
            }
            LauncherService.atomic(temp,pack);
        }finally{Files.deleteIfExists(temp);}
        return new Result(parents,rotations,patched.size());
    }

    /** A parent that Minecraft cannot resolve: no namespace and not inside a vanilla model folder. */
    static boolean brokenParent(String parent){
        if(parent.contains(":"))return false;
        return !(parent.startsWith("item/")||parent.startsWith("block/")||parent.startsWith("builtin/"));
    }
    static boolean hasLayer0(JsonObject o){return o.has("textures")&&o.get("textures").isJsonObject()&&o.getAsJsonObject("textures").has("layer0");}
    static String namespace(String entry){return entry.substring(7,entry.indexOf('/',7));}
    static String baseName(String path){String n=path.substring(path.lastIndexOf('/')+1);return n.endsWith(".json")?n.substring(0,n.length()-5):n;}

    /** Snaps each element rotation angle to the nearest value Minecraft accepts. Returns how many were changed. */
    static int fixRotations(JsonObject model){
        int fixed=0;
        if(!model.has("elements")||!model.get("elements").isJsonArray())return 0;
        for(JsonElement el:model.getAsJsonArray("elements")){
            if(!el.isJsonObject()||!el.getAsJsonObject().has("rotation")||!el.getAsJsonObject().get("rotation").isJsonObject())continue;
            JsonObject rotation=el.getAsJsonObject().getAsJsonObject("rotation");
            if(!rotation.has("angle")||!rotation.get("angle").isJsonPrimitive())continue;
            double angle=rotation.get("angle").getAsDouble();
            double best=ANGLES[0];for(double a:ANGLES)if(Math.abs(a-angle)<Math.abs(best-angle))best=a;
            if(best!=angle){rotation.addProperty("angle",best);fixed++;}
        }
        return fixed;
    }

    private static void put(ZipOutputStream out,String name,String text)throws IOException {
        out.putNextEntry(new ZipEntry(name));out.write(text.getBytes(StandardCharsets.UTF_8));out.closeEntry();
    }

    /** True when a running process was started with this game directory (so options.txt would be overwritten on exit). */
    static boolean gameRunning(Path gameDir){
        String needle=gameDir.toAbsolutePath().toString().toLowerCase();
        return ProcessHandle.allProcesses().anyMatch(p->p.info().commandLine().map(c->c.toLowerCase().contains(needle)).orElse(false));
    }

    /** Adds the fix pack to options.txt's resourcePacks list if missing. Returns false when the game owns the file right now. */
    static boolean enable(Path gameDir)throws IOException {
        if(gameRunning(gameDir))return false;
        Path options=gameDir.resolve("options.txt");
        List<String> lines=Files.isRegularFile(options)?new ArrayList<>(Files.readAllLines(options,StandardCharsets.UTF_8)):new ArrayList<>();
        String entry="\""+PACK_ENTRY+"\"";
        for(int i=0;i<lines.size();i++){
            if(!lines.get(i).startsWith("resourcePacks:"))continue;
            String list=lines.get(i).substring("resourcePacks:".length()).trim();
            if(list.contains(entry))return true;
            String inner=list.startsWith("[")&&list.endsWith("]")?list.substring(1,list.length()-1).trim():"";
            lines.set(i,"resourcePacks:["+(inner.isEmpty()?"":inner+",")+entry+"]");
            Files.write(options,lines,StandardCharsets.UTF_8);return true;
        }
        lines.add("resourcePacks:["+entry+"]");
        Files.write(options,lines,StandardCharsets.UTF_8);return true;
    }
}
