import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.regex.*;
import java.util.zip.*;

/**
 * Dependency-aware mod scanner and sorter for a Forge 1.20.1 mods folder.
 * Reads META-INF/mods.toml straight from each jar (no extraction), classifies, orders and diagnoses.
 * Same canon categories and tie-breaks as the in-game Mine Canon hub, so both views agree.
 */
final class ModSorter {
    static final String MC="1.20.1", FORGE="47.4.26";

    enum Category {
        CORE("Core",0xA4A7AD),LIBRARY("Library",0x6FA8DC),OPTIMIZATION("Optimization",0xA8CF8A),INTERFACE("Interface",0xC9A0FF),CONTENT("Content",0xF57C2C),CANON("Canon",0xFFD27A),UNKNOWN("Unreadable",0xE05A5A);
        final String label;final int rgb;Category(String l,int c){label=l;rgb=c;}
    }
    enum Mode {LOAD_PLAN("Load plan"),CATEGORY("Category"),NAME("Name"),SIZE("Size"),DEPENDENTS("Dependents");
        final String label;Mode(String l){label=l;}Mode next(){return values()[(ordinal()+1)%values().length];}}
    enum Severity {ERROR,WARN,INFO}
    record Issue(Severity severity,String modId,String text){}

    static final class Dep {String owner;String modId;boolean mandatory;String range="";String side="BOTH";}
    static final class Mod {
        Path file;String fileName;boolean enabled=true;long size;
        String loader="forge";// forge | fabric | neoforge | quilt | none
        String id="",name="",version="",description="";
        final List<Dep> deps=new ArrayList<>();final Map<String,String> bundled=new LinkedHashMap<>();// jar-in-jar mods: id -> version
        final List<String> requires=new ArrayList<>(),optional=new ArrayList<>(),dependents=new ArrayList<>();
        Category category=Category.CONTENT;int plan=-1;
        boolean readable(){return !id.isEmpty()&&loader.equals("forge");}
    }
    static final class Result {
        final List<Mod> mods=new ArrayList<>();final List<Issue> issues=new ArrayList<>();
        long count(Severity s){return issues.stream().filter(i->i.severity==s).count();}
    }

    // ---------- scanning ----------
    static Result scan(Path modsDir)throws IOException {
        Result r=new Result();
        if(!Files.isDirectory(modsDir))return r;
        List<Path> files;try(var s=Files.list(modsDir)){files=s.filter(Files::isRegularFile).sorted().toList();}
        for(Path f:files){
            String n=f.getFileName().toString(),l=n.toLowerCase(Locale.ROOT);
            boolean on=l.endsWith(".jar"),off=l.endsWith(".jar.disabled");
            if(!on&&!off)continue;
            Mod m=readJar(f);m.enabled=on;r.mods.add(m);
        }
        analyse(r);
        return r;
    }

    static Mod readJar(Path f){
        Mod m=new Mod();m.file=f;m.fileName=f.getFileName().toString();
        try{m.size=Files.size(f);}catch(IOException ignored){}
        try(ZipFile z=new ZipFile(f.toFile())){
            ZipEntry toml=z.getEntry("META-INF/mods.toml");
            if(toml==null){
                if(z.getEntry("fabric.mod.json")!=null)m.loader="fabric";
                else if(z.getEntry("quilt.mod.json")!=null)m.loader="quilt";
                else if(z.getEntry("META-INF/neoforge.mods.toml")!=null)m.loader="neoforge";
                else m.loader="none";
                m.name=stem(m.fileName);return m;
            }
            String text;try(var in=z.getInputStream(toml)){text=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
            String jarVersion=null;ZipEntry mf=z.getEntry("META-INF/MANIFEST.MF");
            if(mf!=null)try(var in=z.getInputStream(mf)){var mani=new java.util.jar.Manifest(in);jarVersion=mani.getMainAttributes().getValue("Implementation-Version");}catch(Exception ignored){}
            parseToml(text,m,jarVersion);
            readBundled(z,m);
            if(m.name.isEmpty())m.name=m.id.isEmpty()?stem(m.fileName):m.id;
        }catch(IOException|RuntimeException e){m.loader="none";m.name=stem(m.fileName);m.description="Could not read this file as a jar: "+e.getMessage();}
        return m;
    }
    /** Forge resolves META-INF/jarjar/*.jar libraries itself, so their mod ids count as present. */
    static void readBundled(ZipFile z,Mod m){
        for(var en=z.entries();en.hasMoreElements();){
            ZipEntry e=en.nextElement();String n=e.getName();
            if(!n.startsWith("META-INF/jarjar/")||!n.toLowerCase(Locale.ROOT).endsWith(".jar"))continue;
            try(var zin=new ZipInputStream(z.getInputStream(e))){
                String jv=null,toml=null;ZipEntry in;
                while((in=zin.getNextEntry())!=null){
                    if(in.getName().equals("META-INF/mods.toml"))toml=new String(zin.readAllBytes(),StandardCharsets.UTF_8);
                    else if(in.getName().equals("META-INF/MANIFEST.MF"))try{jv=new java.util.jar.Manifest(new ByteArrayInputStream(zin.readAllBytes())).getMainAttributes().getValue("Implementation-Version");}catch(Exception ignored){}
                }
                if(toml==null)continue;
                Mod inner=new Mod();parseToml(toml,inner,jv);
                if(!inner.id.isEmpty())m.bundled.put(inner.id,inner.version.isEmpty()?"?":inner.version);
            }catch(IOException|RuntimeException ignored){}
        }
    }
    static String stem(String n){return n.replaceAll("(?i)\\.jar(\\.disabled)?$","");}

    /** Minimal mods.toml reader: [[mods]] and [[dependencies.<id>]] tables with string/bool values, including """ blocks. */
    static void parseToml(String text,Mod m,String jarVersion){
        String table="";Map<String,String> cur=null;boolean firstMod=true;Dep dep=null;
        String[] lines=text.replace("\r","").split("\n");
        for(int i=0;i<lines.length;i++){
            String line=lines[i].strip();
            if(line.isEmpty()||line.startsWith("#"))continue;
            if(line.startsWith("[[")){
                int end=line.indexOf("]]");table=line.substring(2,end<0?line.length():end).strip();dep=null;
                if(table.startsWith("dependencies.")){dep=new Dep();dep.modId="";m.deps.add(dep);ownerOf(table,m,dep);}
                cur=null;continue;
            }
            if(line.startsWith("[")){table=line.replaceAll("[\\[\\]]","").strip();continue;}
            int eq=line.indexOf('=');if(eq<0)continue;
            String key=line.substring(0,eq).strip(),val=line.substring(eq+1).strip();
            if(val.startsWith("'''")||val.startsWith("\"\"\"")){
                String q=val.substring(0,3);String body=val.substring(3);
                if(body.contains(q))body=body.substring(0,body.indexOf(q));
                else{StringBuilder sb=new StringBuilder(body);while(++i<lines.length){String ln=lines[i];int e=ln.indexOf(q);if(e>=0){sb.append("\n").append(ln,0,e);break;}sb.append("\n").append(ln);}body=sb.toString();}
                val=body.strip();
            }else val=unquote(val);
            if(table.equals("mods")){
                if(!firstMod&&key.equals("modId"))continue;// only the first mod in a file is classified
                switch(key){
                    case "modId"->{if(m.id.isEmpty())m.id=val;else firstMod=false;}
                    case "displayName"->{if(m.name.isEmpty())m.name=val;}
                    case "version"->{if(m.version.isEmpty())m.version=val.contains("${")?(jarVersion==null?"?":jarVersion):val;}
                    case "description"->{if(m.description.isEmpty())m.description=val.replaceAll("\\s+"," ").strip();}
                    default->{}
                }
            }else if(dep!=null){
                switch(key){
                    case "modId"->dep.modId=val;
                    case "mandatory"->dep.mandatory=val.equalsIgnoreCase("true");
                    case "type"->dep.mandatory=val.equalsIgnoreCase("required");
                    case "versionRange"->dep.range=val;
                    case "side"->dep.side=val;
                    default->{}
                }
            }
        }
        // keep only dependencies that belong to this jar's primary mod
        m.deps.removeIf(d->d.modId.isEmpty()||d.owner!=null&&!d.owner.equals(m.id));
    }
    private static void ownerOf(String table,Mod m,Dep d){d.owner=table.substring("dependencies.".length()).strip();}
    private static String unquote(String v){
        int hash=-1;boolean q=false;char qc=0;
        for(int i=0;i<v.length();i++){char c=v.charAt(i);if(!q&&(c=='"'||c=='\'')){q=true;qc=c;}else if(q&&c==qc&&(i==0||v.charAt(i-1)!='\\'))q=false;else if(!q&&c=='#'){hash=i;break;}}
        if(hash>=0)v=v.substring(0,hash).strip();
        if(v.length()>=2&&(v.startsWith("\"")&&v.endsWith("\"")||v.startsWith("'")&&v.endsWith("'")))v=v.substring(1,v.length()-1);
        return v;
    }

    // ---------- analysis ----------
    static void analyse(Result r){
        r.issues.clear();
        Map<String,List<Mod>> byId=new LinkedHashMap<>();
        for(Mod m:r.mods)if(m.readable())byId.computeIfAbsent(m.id,k->new ArrayList<>()).add(m);
        // loader / unreadable files
        for(Mod m:r.mods){
            if(m.readable()||!m.enabled)continue;
            String why=switch(m.loader){
                case "fabric"->"is a Fabric mod and will not load under Forge.";
                case "quilt"->"is a Quilt mod and will not load under Forge.";
                case "neoforge"->"is a NeoForge-only mod and will not load under Forge 1.20.1.";
                default->"has no Forge mods.toml (not a Forge mod, or a library jar-in-jar).";
            };
            r.issues.add(new Issue(Severity.WARN,m.fileName,m.fileName+" "+why));
            m.category=Category.UNKNOWN;
        }
        // duplicates among enabled
        Map<String,Mod> active=new LinkedHashMap<>();
        for(var e:byId.entrySet()){
            List<Mod> on=e.getValue().stream().filter(x->x.enabled).toList();
            if(on.size()>1)r.issues.add(new Issue(Severity.ERROR,e.getKey(),"Duplicate mod id '"+e.getKey()+"' in "+on.stream().map(x->x.fileName).toList()+". Forge refuses to start with duplicates; disable one."));
            if(!on.isEmpty())active.put(e.getKey(),on.get(0));
        }
        // edges
        for(Mod m:r.mods){m.requires.clear();m.optional.clear();m.dependents.clear();}
        for(Mod m:active.values()){
            for(Dep d:m.deps){
                if(d.modId.equals("minecraft")||d.modId.equals("forge")){
                    String actual=d.modId.equals("minecraft")?MC:FORGE;
                    if(d.mandatory&&!d.range.isEmpty()&&!Range.contains(d.range,actual))
                        r.issues.add(new Issue(Severity.ERROR,m.id,m.name+" needs "+d.modId+" "+d.range+" but this launcher runs "+actual+"."));
                    continue;
                }
                Mod target=active.get(d.modId);
                if(target==null){
                    if(!d.mandatory)continue;
                    if(providedByJarJar(active.values(),d))continue;
                    if(d.side.equalsIgnoreCase("SERVER"))continue;
                    List<Mod> off=byId.getOrDefault(d.modId,List.of());
                    if(!off.isEmpty())r.issues.add(new Issue(Severity.ERROR,m.id,m.name+" needs '"+d.modId+"', which is installed but disabled ("+off.get(0).fileName+"). Enable it."));
                    else r.issues.add(new Issue(Severity.ERROR,m.id,m.name+" needs '"+d.modId+"' "+d.range+", which is not installed."));
                    continue;
                }
                if(target==m)continue;
                if(d.mandatory){
                    if(!m.requires.contains(target.id)){m.requires.add(target.id);target.dependents.add(m.id);}
                    if(!d.range.isEmpty()&&!target.version.isEmpty()&&!target.version.equals("?")&&!Range.contains(d.range,target.version))
                        r.issues.add(new Issue(Severity.WARN,m.id,m.name+" wants "+d.modId+" "+d.range+" but "+target.version+" is installed."));
                }else if(!m.optional.contains(target.id))m.optional.add(target.id);
            }
        }
        for(Mod m:r.mods){
            if(!m.readable())continue;
            m.category=classify(m.id,m.name,m.dependents.size());
        }
        // plan (Kahn; only enabled readable mods)
        List<Mod> pool=new ArrayList<>(active.values());
        plan(pool);
        int n=pool.size();int i=n;
        for(Mod m:r.mods)if(m.plan<0||!active.containsValue(m)){m.plan=i++;}
        // cycles
        Set<Mod> stuck=cycleMembers(pool);
        if(!stuck.isEmpty())r.issues.add(new Issue(Severity.ERROR,"cycle","Dependency cycle among: "+stuck.stream().map(x->x.id).sorted().toList()+"."));
        // disabled notes
        long off=r.mods.stream().filter(x->!x.enabled).count();
        if(off>0)r.issues.add(new Issue(Severity.INFO,"disabled",off+" mod file(s) disabled (.jar.disabled); they are skipped by Forge."));
        if(r.issues.stream().noneMatch(x->x.severity!=Severity.INFO))r.issues.add(0,new Issue(Severity.INFO,"ok","No dependency problems found among "+active.size()+" enabled Forge mod(s). Range checks cover only what mods.toml declares."));
    }

    private static boolean providedByJarJar(Collection<Mod> mods,Dep d){
        for(Mod x:mods){String v=x.bundled.get(d.modId);if(v!=null&&(d.range.isEmpty()||v.equals("?")||Range.contains(d.range,v)))return true;}
        return false;
    }

    static void plan(List<Mod> mods){
        Map<String,Mod> by=new HashMap<>();for(Mod m:mods){by.put(m.id,m);m.plan=-1;}
        Map<String,Integer> indeg=new HashMap<>();
        for(Mod m:mods)indeg.put(m.id,(int)m.requires.stream().filter(by::containsKey).distinct().count());
        PriorityQueue<Mod> ready=new PriorityQueue<>(TIE);
        for(Mod m:mods)if(indeg.get(m.id)==0)ready.add(m);
        int i=0;Set<String> done=new HashSet<>();
        while(!ready.isEmpty()){
            Mod m=ready.poll();m.plan=i++;done.add(m.id);
            for(String child:new LinkedHashSet<>(m.dependents)){
                Mod c=by.get(child);if(c==null||done.contains(c.id))continue;
                if(indeg.merge(c.id,-1,Integer::sum)==0)ready.add(c);
            }
        }
        List<Mod> rest=new ArrayList<>();for(Mod m:mods)if(m.plan<0)rest.add(m);
        rest.sort(TIE);for(Mod m:rest)m.plan=i++;
    }
    static final Comparator<Mod> TIE=Comparator.comparingInt((Mod m)->m.category.ordinal()).thenComparing(m->m.name.toLowerCase(Locale.ROOT)).thenComparing(m->m.id);
    static Set<Mod> cycleMembers(List<Mod> mods){
        Map<String,Mod> by=new HashMap<>();for(Mod m:mods)by.put(m.id,m);
        Map<String,Integer> indeg=new HashMap<>();for(Mod m:mods)indeg.put(m.id,(int)m.requires.stream().filter(by::containsKey).count());
        Deque<Mod> q=new ArrayDeque<>();for(Mod m:mods)if(indeg.get(m.id)==0)q.add(m);
        Set<String> seen=new HashSet<>();
        while(!q.isEmpty()){Mod m=q.poll();seen.add(m.id);for(String c:m.dependents){Mod cm=by.get(c);if(cm!=null&&indeg.merge(c,-1,Integer::sum)==0)q.add(cm);}}
        Set<Mod> out=new LinkedHashSet<>();for(Mod m:mods)if(!seen.contains(m.id))out.add(m);return out;
    }

    static final Set<String> LIB=Set.of("architectury","cloth_config","geckolib","kotlinforforge","balm","bookshelf","playeranimator","creativecore","supermartijn642corelib","curios","collective","citadel","moonlight","resourcefullib","patchouli","fusion","puzzleslib","forgeconfigapiport","configured_lib");
    static final Set<String> OPT=Set.of("embeddium","rubidium","oculus","ferritecore","modernfix","canary","starlight","entityculling","immediatelyfast","smoothboot","memoryleakfix","saturn","dynamic_fps","sodiumdynamiclights","noisium","badoptimizations","cull_less_leaves","lazydfu","clumps","fastsuite");
    static final Set<String> UI=Set.of("jei","roughlyenoughitems","journeymap","xaerominimap","xaeroworldmap","jade","waila","appleskin","mousetweaks","controlling","configured","catalogue","inventoryhud","emi","jeresources","betterf3","invtweaks","inventoryprofilesnext","craftingtweaks","itemzoom");
    static Category classify(String id,String displayName,int dependents){
        String i=id.toLowerCase(Locale.ROOT),n=displayName==null?"":displayName.toLowerCase(Locale.ROOT);
        if(i.equals("minecraft")||i.equals("forge"))return Category.CORE;
        if(i.startsWith("minecanon")||i.startsWith("canon"))return Category.CANON;
        if(LIB.contains(i)||i.endsWith("lib")||i.endsWith("_lib")||i.endsWith("-lib")||i.endsWith("api")||n.endsWith(" lib")||n.endsWith(" library")||n.endsWith(" api")||n.contains("core lib"))return Category.LIBRARY;
        if(OPT.contains(i)||n.contains("optimiz")||n.contains("performance"))return Category.OPTIMIZATION;
        if(UI.contains(i)||n.contains("minimap")||n.contains("tooltip")||n.contains("inventory"))return Category.INTERFACE;
        if(dependents>=2&&(i.contains("core")||i.contains("common")))return Category.LIBRARY;
        return Category.CONTENT;
    }

    static List<Mod> sorted(Collection<Mod> mods,Mode mode){
        List<Mod> l=new ArrayList<>(mods);
        Comparator<Mod> name=Comparator.comparing((Mod m)->m.name.toLowerCase(Locale.ROOT)).thenComparing(m->m.fileName);
        switch(mode){
            case NAME->l.sort(name);
            case CATEGORY->l.sort(Comparator.comparingInt((Mod m)->m.category.ordinal()).thenComparing(name));
            case SIZE->l.sort(Comparator.comparingLong((Mod m)->-m.size).thenComparing(name));
            case DEPENDENTS->l.sort(Comparator.comparingInt((Mod m)->-m.dependents.size()).thenComparing(name));
            default->l.sort(Comparator.comparingInt((Mod m)->m.plan));
        }
        return l;
    }

    static String human(long b){return b<=0?"-":b<1048576?String.format(Locale.ROOT,"%.0f KB",b/1024.0):String.format(Locale.ROOT,"%.1f MB",b/1048576.0);}

    /** Safe rename between .jar and .jar.disabled; refuses to overwrite. */
    static Path toggle(Mod m)throws IOException {
        String n=m.fileName;Path target=m.file.resolveSibling(m.enabled?n+".disabled":n.substring(0,n.length()-".disabled".length()));
        if(Files.exists(target))throw new IOException(target.getFileName()+" already exists; refusing to overwrite.");
        return Files.move(m.file,target);
    }

    static String exportPlan(Result r,String instance){
        StringBuilder sb=new StringBuilder("# Mine Canon load plan - "+instance+"\n# Minecraft "+MC+" / Forge "+FORGE+"\n\n");
        for(Mod m:sorted(r.mods.stream().filter(x->x.enabled&&x.readable()).toList(),Mode.LOAD_PLAN))
            sb.append(String.format(Locale.ROOT,"%2d. %-28s %-12s needs %-30s %s%n",m.plan+1,m.name,m.category.label,m.requires.isEmpty()?"-":String.join(",",m.requires),m.fileName));
        sb.append("\n# Issues\n");for(Issue i:r.issues)sb.append(i.severity).append("  ").append(i.text).append("\n");
        return sb.toString();
    }

    // ---------- Maven version ranges (what mods.toml uses) ----------
    static final class Range {
        static boolean contains(String spec,String version){
            spec=spec.strip();if(spec.isEmpty())return true;
            if(!spec.startsWith("[")&&!spec.startsWith("("))return compare(version,spec)>=0;// bare = soft minimum
            Matcher mt=Pattern.compile("([\\[(])([^,\\])]*)(?:,([^\\])]*))?([\\])])").matcher(spec);
            boolean any=false;
            while(mt.find()){
                any=true;String lo=mt.group(2).strip(),hi=mt.group(3);boolean loInc=mt.group(1).equals("["),hiInc=mt.group(4).equals("]");
                if(hi==null){if(compare(version,lo)==0)return true;continue;}
                hi=hi.strip();
                boolean okLo=lo.isEmpty()||(loInc?compare(version,lo)>=0:compare(version,lo)>0);
                boolean okHi=hi.isEmpty()||(hiInc?compare(version,hi)<=0:compare(version,hi)<0);
                if(okLo&&okHi)return true;
            }
            return !any;
        }
        static int compare(String a,String b){
            List<String> x=parts(a),y=parts(b);
            for(int i=0;i<Math.max(x.size(),y.size());i++){
                String p=i<x.size()?x.get(i):"0",q=i<y.size()?y.get(i):"0";
                boolean pn=p.chars().allMatch(Character::isDigit),qn=q.chars().allMatch(Character::isDigit);
                int c=pn&&qn?new java.math.BigInteger(p).compareTo(new java.math.BigInteger(q)):pn?1:qn?-1:p.compareToIgnoreCase(q);
                if(c!=0)return c;
            }
            return 0;
        }
        private static List<String> parts(String v){
            int cut=v.indexOf('-');String core=cut<0?v:v.substring(0,cut);
            List<String> out=new ArrayList<>(Arrays.asList(core.split("[.+_]")));out.removeIf(String::isEmpty);return out;
        }
    }
}
