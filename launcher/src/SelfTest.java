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
 static void jar(Path dir,String file,String entry,String body)throws Exception {Files.createDirectories(dir);try(var z=new java.util.zip.ZipOutputStream(Files.newOutputStream(dir.resolve(file)))){z.putNextEntry(new java.util.zip.ZipEntry(entry));z.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));z.closeEntry();}}
 static String toml(String id,String ver,String name,String... deps){StringBuilder b=new StringBuilder("modLoader=\"javafml\"\nloaderVersion=\"[47,)\"\nlicense=\"MIT\"\n[[mods]]\nmodId=\""+id+"\"\nversion=\""+ver+"\"\ndisplayName=\""+name+"\"\ndescription='''\nline one\nline two\n'''\n");for(int i=0;i<deps.length;i+=3)b.append("[[dependencies."+id+"]]\nmodId=\""+deps[i]+"\"\nmandatory="+deps[i+1]+"\nversionRange=\""+deps[i+2]+"\"\nside=\"BOTH\"\n");return b.toString();}
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

  // ---- dependency-aware mod sorter (v2: load plan, diagnostics, enable/disable) ----
  Path md=temp.resolve("sortmods");
  jar(md,"zeta-app-1.0.jar","META-INF/mods.toml",toml("zetaapp","1.0","Zeta App","zetalib","true","[2.0,3.0)","minecraft","true","[1.20.1,1.21)","forge","true","[47,)"));
  jar(md,"zetalib-2.5.jar","META-INF/mods.toml",toml("zetalib","2.5.0","Zeta Lib"));
  jar(md,"alpha-1.0.jar","META-INF/mods.toml",toml("alpha","1.0","Alpha Content","zetaapp","true","","needsoff","true","[1,)"));
  jar(md,"needsoff-1.0.jar.disabled","META-INF/mods.toml",toml("needsoff","1.0","Needs Off"));
  jar(md,"ghost-1.0.jar","META-INF/mods.toml",toml("ghostmod","1.0","Ghost","nothere","true","[1,)","optionalthing","false","[1,)"));
  jar(md,"dupe-a.jar","META-INF/mods.toml",toml("dupe","1.0","Dupe A"));jar(md,"dupe-b.jar","META-INF/mods.toml",toml("dupe","1.1","Dupe B"));
  jar(md,"old-1.12.jar","META-INF/mods.toml",toml("oldmod","1.0","Old Mod","minecraft","true","[1.12,1.13)"));
  jar(md,"fabricy.jar","fabric.mod.json","{\"id\":\"fabricy\"}");
  jar(md,"cycA.jar","META-INF/mods.toml",toml("cyca","1","Cyc A","cycb","true",""));jar(md,"cycB.jar","META-INF/mods.toml",toml("cycb","1","Cyc B","cyca","true",""));
  Files.writeString(md.resolve("notes.txt"),"x");Files.writeString(md.resolve("broken.jar"),"not a zip");
  final ModSorter.Result res=ModSorter.scan(md);
  java.util.function.Function<String,ModSorter.Mod> byId=id->res.mods.stream().filter(x->id.equals(x.id)&&x.enabled).findFirst().orElse(null);
  java.util.function.Predicate<String> has=t->res.issues.stream().anyMatch(i->i.text().contains(t));
  check(res.mods.size()==12,"scanner lists .jar and .jar.disabled only");
  check(byId.apply("zetalib").plan<byId.apply("zetaapp").plan&&byId.apply("zetaapp").plan<byId.apply("alpha").plan,"load plan puts dependencies first");
  check(byId.apply("zetaapp").requires.equals(java.util.List.of("zetalib"))&&byId.apply("zetalib").dependents.contains("zetaapp"),"dependency edges resolved both ways");
  check(byId.apply("zetaapp").description.equals("line one line two"),"multi-line description parsed");
  check(has.test("needs 'nothere'"),"missing mandatory dependency reported");
  check(has.test("installed but disabled"),"disabled-but-needed dependency reported");
  check(has.test("Duplicate mod id 'dupe'"),"duplicate mod id reported");
  check(has.test("needs minecraft [1.12,1.13)"),"wrong Minecraft range reported");
  check(has.test("Fabric mod")&&has.test("no Forge mods.toml"),"fabric and unreadable jars flagged");
  check(has.test("Dependency cycle")&&has.test("cyca")&&has.test("cycb"),"dependency cycle detected");
  check(!has.test("optionalthing"),"optional missing dependency stays quiet");
  check(res.issues.stream().noneMatch(i->i.text().contains("wants zetalib")),"satisfied version range raises no warning");
  jar(md,"zetalib-2.5.jar","META-INF/mods.toml",toml("zetalib","3.1.0","Zeta Lib"));ModSorter.Result r2=ModSorter.scan(md);
  check(r2.issues.stream().anyMatch(i->i.text().contains("wants zetalib [2.0,3.0) but 3.1.0")),"installed version outside declared range warned");
  ModSorter.Mod off=r2.mods.stream().filter(x->!x.enabled).findFirst().get();Path moved=ModSorter.toggle(off);check(moved.getFileName().toString().equals("needsoff-1.0.jar")&&Files.exists(moved),"enable renames .jar.disabled to .jar");
  ModSorter.Mod back=ModSorter.scan(md).mods.stream().filter(x->"needsoff".equals(x.id)).findFirst().get();Path again=ModSorter.toggle(back);check(again.getFileName().toString().endsWith(".jar.disabled"),"disable renames .jar to .jar.disabled");
  Files.writeString(md.resolve("needsoff-1.0.jar"),"collide");ModSorter.Mod b2=ModSorter.scan(md).mods.stream().filter(x->x.fileName.endsWith(".disabled")).findFirst().get();final ModSorter.Mod b3=b2;expectFailure(()->ModSorter.toggle(b3),"toggle refuses to overwrite an existing file");
  check(ModSorter.Range.contains("[47,)","47.4.26")&&!ModSorter.Range.contains("[1.20.1,1.21)","1.21")&&ModSorter.Range.contains("[1.20.1]","1.20.1")&&ModSorter.Range.contains("(,1.0],[1.2,)","1.5"),"Maven version ranges evaluate correctly");
  check(ModSorter.Range.compare("1.10","1.9")>0&&ModSorter.Range.compare("2.0","2.0.0")==0,"version comparison is numeric, not lexical");
  java.io.ByteArrayOutputStream inner=new java.io.ByteArrayOutputStream();try(var z=new java.util.zip.ZipOutputStream(inner)){z.putNextEntry(new java.util.zip.ZipEntry("META-INF/mods.toml"));z.write(toml("innerlib","1.5","Inner Lib").getBytes());z.closeEntry();}
  try(var z=new java.util.zip.ZipOutputStream(Files.newOutputStream(md.resolve("host.jar")))){z.putNextEntry(new java.util.zip.ZipEntry("META-INF/mods.toml"));z.write(toml("hostmod","1","Host","innerlib","true","[1,2)").getBytes());z.closeEntry();z.putNextEntry(new java.util.zip.ZipEntry("META-INF/jarjar/innerlib-1.5.jar"));z.write(inner.toByteArray());z.closeEntry();}
  ModSorter.Result r3=ModSorter.scan(md);check(r3.issues.stream().noneMatch(i->i.text().contains("innerlib")),"jar-in-jar library satisfies a mandatory dependency");check(r3.mods.stream().filter(x->"hostmod".equals(x.id)).findFirst().get().bundled.containsKey("innerlib"),"bundled mod id is recorded");
  Files.delete(md.resolve("host.jar"));jar(md,"host.jar","META-INF/mods.toml",toml("hostmod","1","Host","innerlib","true","[1,2)"));check(ModSorter.scan(md).issues.stream().anyMatch(i->i.text().contains("needs 'innerlib'")),"same dependency without the bundle is reported missing");
  check(LauncherService.AGE_TAGS.length==LauncherService.IDS.length,"every age has a minecanon.age tag");
  check(own.get("javaArgs").getAsString().contains("-Dminecanon.age=1"),"profile passes the age tag to the in-game mod");
  var audit=CanonModel.audit(s);
  check(audit.get(1).pass(),"all five bundled source hashes verified");
  check(audit.get(4).pass(),"finite-era inflation arithmetic passes scoped bound");
  check(!audit.get(5).pass()&&audit.get(5).status().equals("EXCEPTION"),"Nocturne margin exception is not falsely certified");
  check(!audit.get(6).pass(),"formal topology claims remain unverified");
  check(CanonModel.rank(349).equals("Initiate")&&CanonModel.rank(350).equals("Hunter")&&CanonModel.rank(790).equals("Anomaly"),"classification boundaries are deterministic");
  SwingUtilities.invokeAndWait(()->{try{LauncherUI ui=new LauncherUI(s);ui.setSize(1536,1024);MineCanonLauncher.layout(ui);ui.eraButtons.get(0).doClick();check(s.selected()==0,"native era button updates saved selection");ui.sidebar.nav[1].doClick();check(ui.current.equals("Worlds"),"Worlds navigation works");ui.sidebar.nav[2].doClick();check(ui.current.equals("Mods"),"Mods navigation works");ui.sidebar.nav[3].doClick();check(ui.current.equals("Load plan"),"Load plan navigation works");ui.sidebar.nav[4].doClick();check(ui.current.equals("Settings"),"Settings navigation works");ui.sidebar.nav[0].doClick();check(ui.current.equals("Home"),"Home navigation works");ui.lineage.nodes[6].doClick();check(s.selected()==3,"lineage node selects Nen instance");ui.classification.bst.setValue(790);check(ui.classification.rank.getText().startsWith("Anomaly"),"BST editor updates classification");ui.sidebar.nav[5].doClick();check(ui.current.equals("Verification"),"Verification navigation works");ui.sidebar.nav[6].doClick();check(ui.current.equals("Boot log"),"Boot log navigation works");check(ui.canon.log.stream().anyMatch(x->x.contains("[LORE]"))&&ui.canon.log.stream().anyMatch(x->x.contains("[LOCAL]")),"narrative and real events are separately labeled");ui.showPage("Home");ui.setSize(1120,820);MineCanonLauncher.layout(ui);for(String tab:new String[]{"Worlds","Mods","Load plan","Settings","Verification","Boot log"}){ui.showPage(tab);MineCanonLauncher.layout(ui);MineCanonLauncher.layout(ui);check(geometry(ui),tab+" layout has no negative component dimensions at minimum size");}ui.showPage("Home");MineCanonLauncher.layout(ui);check(ui.open.getX()>=0&&ui.open.getX()+ui.open.getWidth()<=ui.launch.getWidth(),"launch action stays within minimum window");}catch(Exception e){throw new RuntimeException(e);}});
  System.out.println("RESULT: "+checks+" checks passed.");
  try(var stream=Files.walk(temp)){for(Path f:stream.sorted(Comparator.reverseOrder()).toList())Files.delete(f);}
 }
}
