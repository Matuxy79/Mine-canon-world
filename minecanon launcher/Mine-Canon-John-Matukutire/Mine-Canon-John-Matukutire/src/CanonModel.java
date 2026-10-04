import java.nio.file.*;
import java.security.*;
import java.time.*;
import java.util.*;

final class CanonModel {
    static final int[] CEILING={540,570,620,660,700,800};
    static final int[] OUTLIER_MIN={620,640,700,740,780,880};
    static final int[] OUTLIER_MAX={660,680,740,780,820,-1};
    static final String[] AGE={"Age 1","Age 2","Age 3","Age 4","Age 4+","Age 5"};
    static String rank(int bst){if(bst<280)return "Below draft ladder";if(bst<350)return "Initiate";if(bst<430)return "Hunter";if(bst<500)return "Veteran";if(bst<570)return "Elite";if(bst<640)return "Master";if(bst<720)return "Calamity";if(bst<790)return "Disaster / Special Grade";return "Anomaly";}
    static String band(int i){return "Era ceiling "+CEILING[i]+" · outlier "+(OUTLIER_MAX[i]<0?"880+":OUTLIER_MIN[i]+"–"+OUTLIER_MAX[i]);}
    static String assessment(int i,int bst){if(i==5)return "Forbidden tier · standard legality undefined";if(bst<=CEILING[i])return "Within era ceiling";if(bst>=OUTLIER_MIN[i]&&bst<=OUTLIER_MAX[i])return "Within draft outlier band";return "Outside the chosen draft bands";}
    record Check(String name,String status,String detail,boolean pass){}
    static java.util.List<Check> audit(LauncherService s){
        java.util.List<Check> checks=new ArrayList<>();
        try{s.verifyInstaller();checks.add(new Check("Installer SHA-256","MATCH","Matches the exact uploaded Forge JAR; this is integrity, not publisher authentication.",true));}catch(Exception e){checks.add(new Check("Installer SHA-256","FAIL",e.getMessage(),false));}
        Properties manifest=new Properties();int matched=0,total=0;String failure="";
        try(var in=CanonModel.class.getResourceAsStream("/assets/dossier-sha256.properties")){
            if(in==null)throw new Exception("Missing dossier manifest");manifest.load(in);total=manifest.size();
            for(String name:manifest.stringPropertyNames()){
                Path file=s.bundle.resolve("dossier").resolve(name);
                String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
                if(hash.equals(manifest.getProperty(name)))matched++;else failure=name+" differs from the supplied source.";
            }
        }catch(Exception e){failure=e.getMessage();}
        checks.add(new Check("Dossier source hashes",matched==total&&total==5?"MATCH":"REVIEW",matched+" / "+total+" source files match the bundled manifest. "+failure,matched==total&&total==5));
        checks.add(new Check("Forge version metadata",s.forgeInstalled()?"DETECTED":"SETUP REQUIRED",LauncherService.VERSION+" in the selected Minecraft folder. Dependency completeness is not tested.",s.forgeInstalled()));
        checks.add(new Check("Java runtime",Runtime.version().feature()>=17?"PASS":"REQUIRES 17+","Current Java: "+System.getProperty("java.version"),Runtime.version().feature()>=17));
        double max=0;boolean margins=true;
        for(int i=0;i<5;i++){max=Math.max(max,(double)OUTLIER_MAX[i]/CEILING[i]);if(OUTLIER_MIN[i]-CEILING[i]<80||OUTLIER_MAX[i]-CEILING[i]>120)margins=false;}
        checks.add(new Check("Finite era inflation ≤ 1.23",max<=1.23?"PASS":"FAIL",String.format(Locale.ROOT,"Maximum %.4f across five finite rows of §1.2. Dark Continent is excluded: its upper bound is open.",max),max<=1.23));
        checks.add(new Check("Every outlier margin is +80–120",margins?"PASS":"EXCEPTION","§1.2 gives Nocturne West a +70–110 band. Its minimum does not satisfy +80. Source retained unchanged.",margins));
        checks.add(new Check("Topology / algebra guarantees","NOT VERIFIED","Persistence, sheaf consistency, probabilistic balance and win-rate claims need formal models, assumptions and proofs; no theorem prover runs here.",false));
        return checks;
    }
    final java.util.List<String> log=new ArrayList<>();
    void event(LauncherService s,String channel,String message){String line=OffsetDateTime.now().withNano(0)+"  ["+channel+"]  "+message;log.add(line);try{Files.createDirectories(s.home);Files.writeString(s.home.resolve("launcher-session.log"),line+System.lineSeparator(),StandardOpenOption.CREATE,StandardOpenOption.APPEND);}catch(Exception e){log.add("[LOCAL] Session log could not be written: "+e.getMessage());}}
}
