import java.nio.file.*;
import java.util.*;
import com.google.gson.*;
import javax.swing.*;

public class SelfTest {
 static int checks=0;
 static void check(boolean yes,String label){if(!yes)throw new AssertionError(label);checks++;System.out.println("PASS "+label);}
 static void expectFailure(Thrower t,String label)throws Exception{boolean failed=false;try{t.run();}catch(Exception e){failed=true;}check(failed,label);}
 static boolean geometry(java.awt.Component c){if(c.getWidth()<0||c.getHeight()<0)return false;if(c instanceof java.awt.Container n)for(var child:n.getComponents())if(!geometry(child))return false;return true;}
 interface Thrower{void run()throws Exception;}
 static void writeZip(Path file,String entry,String content)throws Exception{try(var z=new java.util.zip.ZipOutputStream(Files.newOutputStream(file))){z.putNextEntry(new java.util.zip.ZipEntry(entry));z.write(content.getBytes(java.nio.charset.StandardCharsets.UTF_8));z.closeEntry();}}
 public static void main(String[] args)throws Exception {
  Path temp=Files.createTempDirectory("minecanon-test-"),bundle=Path.of(args[0]).toAbsolutePath(),mc=temp.resolve("minecraft");Files.createDirectories(mc);
  LauncherService s=new LauncherService(temp.resolve("data"),bundle);s.config.setProperty("minecraft",mc.toString());
  check(!s.forgeInstalled(),"missing Forge is not ready");
  expectFailure(()->s.prepareProfile(0),"profile creation refuses missing Forge");
  Path version=mc.resolve("versions").resolve(LauncherService.VERSION).resolve(LauncherService.VERSION+".json");Files.createDirectories(version.getParent());Files.writeString(version,"{\"id\":\"wrong\"}");check(!s.forgeInstalled(),"wrong version is rejected");
  Files.writeString(version,"{\"id\":\""+LauncherService.VERSION+"\"}");check(s.forgeInstalled(),"matching Forge metadata detected");
  Path profiles=mc.resolve("launcher_profiles.json");String original="{\"profiles\":{\"vanilla\":{\"name\":\"Keep me\",\"custom\":42}},\"settings\":{\"language\":\"en-us\"}}";Files.writeString(profiles,original);
  s.config.setProperty("ram","6");Path backup=s.prepareProfile(0);check(Files.readString(backup).equals(original),"backup preserves exact original bytes");
  JsonObject root=JsonParser.parseString(Files.readString(profiles)).getAsJsonObject(),p=root.getAsJsonObject("profiles");
  check(p.getAsJsonObject("vanilla").get("custom").getAsInt()==42,"unrelated profile fields preserved");check(root.getAsJsonObject("settings").get("language").getAsString().equals("en-us"),"top-level launcher settings preserved");
  JsonObject own=p.getAsJsonObject("mine-canon-first-breath");check(own.get("lastVersionId").getAsString().equals(LauncherService.VERSION),"correct Forge version pinned");check(own.get("javaArgs").getAsString().contains("-Xmx6G"),"memory applied to Minecraft profile");check(Files.isDirectory(Path.of(own.get("gameDir").getAsString()).resolve("mods")),"dedicated game and mods folder created");
  s.prepareProfile(0);root=JsonParser.parseString(Files.readString(profiles)).getAsJsonObject();check(root.getAsJsonObject("profiles").size()==2,"repeat preparation does not duplicate profiles");
  s.prepareProfile(1);root=JsonParser.parseString(Files.readString(profiles)).getAsJsonObject();check(root.getAsJsonObject("profiles").size()==3,"separate eras get separate profiles");check(!s.instance(0).equals(s.instance(1)),"era save directories isolated");
  s.select(2);check(new LauncherService(s.home,bundle).selected()==2,"world selection persists");
  Files.writeString(s.instance(0).resolve("mods/example.jar"),"test");Files.writeString(s.instance(0).resolve("mods/notes.txt"),"test");check(s.mods(0).size()==1,"mods list filters JAR files");
  // ModScanner: loader detection, Minecraft version ranges, auto-sort, in-game state bridge
  check(ModScanner.versionMatches("[1.20.1]","1.20.1")&&ModScanner.versionMatches("[1.20,)","1.20.1")&&ModScanner.versionMatches("[1.20.1,1.21)","1.20.1")&&ModScanner.versionMatches("1.20.1","1.20.1"),"version ranges accept 1.20.1");
  check(!ModScanner.versionMatches("[1.20.4,)","1.20.1")&&!ModScanner.versionMatches("(1.20.1,1.21)","1.20.1"),"version ranges reject other versions");
  Path libs=temp.resolve("library");Files.createDirectories(libs);
  writeZip(libs.resolve("fabric-mod.jar"),"fabric.mod.json","{\"id\":\"fabricmod\",\"version\":\"1.0\",\"name\":\"Fabric Mod\",\"depends\":{\"minecraft\":\"1.20.1\"}}");
  writeZip(libs.resolve("forge-good.jar"),"META-INF/mods.toml","modLoader=\"javafml\"\n[[mods]]\nmodId=\"canonmod\"\nversion=\"1.0\"\ndisplayName=\"Canon Mod\"\n[[dependencies.minecraft]]\nmodId=\"minecraft\"\nversionRange=\"[1.20.1]\"\n");
  writeZip(libs.resolve("plugin.jar"),"plugin.yml","name: Plugin");
  var infos=ModScanner.scanFolder(libs);
  check(infos.size()==3,"scanner finds all three test JARs");
  check(infos.get(0).status()==ModScanner.Status.WRONG_LOADER,"Fabric mod detected as wrong loader");
  check(infos.get(1).status()==ModScanner.Status.COMPATIBLE&&infos.get(1).modId().equals("canonmod"),"Forge 1.20.1 mod detected compatible");
  check(infos.get(2).status()==ModScanner.Status.UNKNOWN,"metadata-less JAR stays unknown");
  s.config.setProperty("library",libs.toString());
  int[] counts=s.sortInto(0,true);
  check(counts[0]==1&&counts[1]==2,"dry-run sort plans one copy and two skips");
  s.sortInto(0,false);check(s.mods(0).size()==2,"sort copies only the compatible mod");
  s.writeState(1);check(JsonParser.parseString(Files.readString(s.stateFile(1))).getAsJsonObject().get("ageName").getAsString().equals(LauncherService.NAMES[1]),"in-game state bridge records the age");
  check(s.verifyInstaller().getFileName().toString().equals(LauncherService.INSTALLER),"supplied installer SHA-256 verified");
  Files.writeString(profiles,"malformed");expectFailure(()->s.prepareProfile(0),"malformed launcher JSON fails safely");check(Files.readString(profiles).equals("malformed"),"malformed source stays untouched");
  Files.writeString(profiles,original);Path store=mc.resolve("launcher_profiles_microsoft_store.json");Files.writeString(store,original);expectFailure(()->s.profilesPath(),"ambiguous profile files require explicit selection");s.config.setProperty("profiles","store");s.prepareProfile(0);check(Files.readString(profiles).equals(original),"Store profile update leaves standard file alone");
  var audit=CanonModel.audit(s);
  check(audit.get(1).pass(),"all five bundled source hashes verified");
  check(audit.get(4).pass(),"finite-era inflation arithmetic passes scoped bound");
  check(!audit.get(5).pass()&&audit.get(5).status().equals("EXCEPTION"),"Nocturne margin exception is not falsely certified");
  check(!audit.get(6).pass(),"formal topology claims remain unverified");
  check(CanonModel.rank(349).equals("Initiate")&&CanonModel.rank(350).equals("Hunter")&&CanonModel.rank(790).equals("Anomaly"),"classification boundaries are deterministic");
  SwingUtilities.invokeAndWait(()->{try{LauncherUI ui=new LauncherUI(s);ui.setSize(1536,1024);MineCanonLauncher.layout(ui);ui.eraButtons.get(0).doClick();check(s.selected()==0,"native era button updates saved selection");ui.sidebar.nav[1].doClick();check(ui.current.equals("Worlds"),"Worlds navigation works");ui.sidebar.nav[2].doClick();check(ui.current.equals("Mods"),"Mods navigation works");ui.sidebar.nav[3].doClick();check(ui.current.equals("Settings"),"Settings navigation works");ui.sidebar.nav[0].doClick();check(ui.current.equals("Home"),"Home navigation works");ui.lineage.nodes[6].doClick();check(s.selected()==3,"lineage node selects Nen instance");ui.classification.bst.setValue(790);check(ui.classification.rank.getText().startsWith("Anomaly"),"BST editor updates classification");ui.sidebar.nav[4].doClick();check(ui.current.equals("Verification"),"Verification navigation works");ui.sidebar.nav[5].doClick();check(ui.current.equals("Boot log"),"Boot log navigation works");check(ui.canon.log.stream().anyMatch(x->x.contains("[LORE]"))&&ui.canon.log.stream().anyMatch(x->x.contains("[LOCAL]")),"narrative and real events are separately labeled");ui.showPage("Home");ui.setSize(1120,820);MineCanonLauncher.layout(ui);for(String tab:new String[]{"Worlds","Mods","Settings","Verification","Boot log"}){ui.showPage(tab);MineCanonLauncher.layout(ui);MineCanonLauncher.layout(ui);check(geometry(ui),tab+" layout has no negative component dimensions at minimum size");}ui.showPage("Home");MineCanonLauncher.layout(ui);check(ui.open.getX()>=0&&ui.open.getX()+ui.open.getWidth()<=ui.launch.getWidth(),"launch action stays within minimum window");}catch(Exception e){throw new RuntimeException(e);}});
  System.out.println("RESULT: "+checks+" checks passed.");
  try(var stream=Files.walk(temp)){for(Path f:stream.sorted(Comparator.reverseOrder()).toList())Files.delete(f);}
 }
}
