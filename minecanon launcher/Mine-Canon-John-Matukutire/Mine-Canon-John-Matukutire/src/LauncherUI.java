import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import java.util.*;
import javax.imageio.ImageIO;

final class LauncherUI extends JPanel {
    static final Color BG=new Color(12,17,21), PANEL=new Color(18,25,29), TEXT=new Color(246,241,230), MUTED=new Color(164,167,173), ORANGE=new Color(245,124,44), LINE=new Color(41,50,57), SAGE=new Color(168,207,138);
    static Font sans(float size){return new Font("SansSerif",Font.PLAIN,(int)size);} static Font serif(float size){return new Font("Serif",Font.BOLD,(int)size);}
    final LauncherService service;final Sidebar sidebar;final JPanel content=new JPanel(null); final JLabel title=new JLabel(), version=new JLabel("1.20.1 / FORGE",SwingConstants.RIGHT), footer=new JLabel();
    final Hero hero; final JPanel eras=new JPanel(new GridLayout(1,3,14,0));final JPanel launch=new JPanel(null);
    final JLabel age=new JLabel(), build=new JLabel("Minecraft 1.20.1  ·  Forge 47.4.26"),hint=new JLabel("Choose your Mine Canon profile in the official launcher.",SwingConstants.RIGHT);
    final ActionButton setup,open;final java.util.List<EraButton> eraButtons=new ArrayList<>();
    JComponent page; String current="Home"; BufferedImage art,eraArt;
    final CanonModel canon=new CanonModel(); java.util.List<CanonModel.Check> checks=new ArrayList<>();
    final CanonPanels.Classification classification; final CanonPanels.Lineage lineage;
    LauncherUI(LauncherService service)throws Exception {
        this.service=service;setLayout(null);setBackground(BG);
        try(var in=getClass().getResourceAsStream("/assets/world.png")){if(in!=null)art=ImageIO.read(in);}
        try(var in=getClass().getResourceAsStream("/assets/ages.png")){if(in!=null)eraArt=ImageIO.read(in);}
        sidebar=new Sidebar();add(sidebar);content.setOpaque(false);add(content);
        title.setForeground(TEXT);title.setFont(serif(42));content.add(title);version.setFont(sans(15));version.setForeground(SAGE);content.add(version);
        footer.setFont(sans(12));footer.setForeground(MUTED);content.add(footer);
        hero=new Hero();classification=new CanonPanels.Classification(this);lineage=new CanonPanels.Lineage(this);
        canon.event(service,"LORE","The Canon Field stirs. Across the ages, one force takes many names.");
        canon.event(service,"LOCAL","Instance selected: "+LauncherService.NAMES[service.selected()]);runAudit();
        eras.setOpaque(false);for(int i=0;i<3;i++){int n=i;EraButton b=new EraButton(i);b.addActionListener(e->select(n));eraButtons.add(b);eras.add(b);}
        launch.setBackground(PANEL);launch.setBorder(new LineBorder(LINE));
        age.setFont(serif(33));age.setForeground(TEXT);build.setFont(sans(18));build.setForeground(MUTED);hint.setFont(sans(14));hint.setForeground(MUTED);
        setup=new ActionButton("Set up Forge",false);setup.addActionListener(e->install());
        open=new ActionButton("OPEN MINECRAFT  ↗",true);open.addActionListener(e->openMinecraft());
        for(Component c:new Component[]{age,build,hint,setup,open})launch.add(c);
        showPage("Home");
    }
    @Override public void paint(Graphics gg){Graphics2D g=(Graphics2D)gg.create();quality(g);super.paint(g);g.dispose();}
    void error(Exception e){JOptionPane.showMessageDialog(this,e.getMessage()==null?e.toString():e.getMessage(),"Mine Canon",JOptionPane.ERROR_MESSAGE);}
    void info(String s){JOptionPane.showMessageDialog(this,s,"Mine Canon",JOptionPane.INFORMATION_MESSAGE);}
    void select(int i){try{service.select(i);canon.event(service,"LOCAL","Instance selected: "+LauncherService.NAMES[i]);canon.event(service,"LORE",LauncherService.LORE[i]);refresh();if(current.equals("Worlds"))showPage("Worlds");}catch(Exception e){error(e);}}
    void runAudit(){checks=CanonModel.audit(service);long passed=checks.stream().filter(CanonModel.Check::pass).count();canon.event(service,"LOCAL","Verification: "+passed+" / "+checks.size()+" checks passed. Read Verification for scope and exceptions.");}
    void refresh(){classification.refresh();age.setText(LauncherService.NAMES[service.selected()]);footer.setText("JAVA "+Runtime.version().feature()+"    /    "+service.status()+"                                      LOCAL INSTANCE");eraButtons.forEach(Component::repaint);repaint();}
    void showPage(String name){
        current=name;if(name.equals("Verification"))runAudit();if(page!=null)content.remove(page);content.remove(hero);content.remove(eras);content.remove(launch);content.remove(classification);content.remove(lineage);
        title.setText(switch(name){case "Worlds"->"Choose your age.";case "Mods"->"Shape your world.";case "Settings"->"Make it yours.";case "Verification"->"Trust, with evidence.";case "Boot log"->"The field awakens.";default->"Your world. Your canon.";});
        if(name.equals("Home")){page=null;content.add(hero);content.add(eras);content.add(launch);content.add(classification);content.add(lineage);}
        else {page=switch(name){case "Worlds"->worlds();case "Mods"->mods();case "Verification"->CanonPanels.verification(this);case "Boot log"->CanonPanels.bootLog(this);default->settings();};content.add(page);}
        refresh();revalidate();repaint();
    }
    @Override public void doLayout(){
        int sw=Math.max(205,Math.min(238,getWidth()*238/1536));sidebar.setBounds(0,0,sw,getHeight());content.setBounds(sw+20,0,getWidth()-sw-40,getHeight());
        int w=content.getWidth(),h=content.getHeight();title.setBounds(20,18,w-175,65);title.setFont(serif(w<1050?34:52));version.setBounds(w-155,43,145,22);
        footer.setBounds(20,h-41,w-25,30);
        if(current.equals("Home")){
            int top=94,launchY=h-125,available=launchY-top-14,rightW=Math.max(290,w/3),leftW=w-rightW-14;
            int tiles=h<900?85:124,info=h<900?166:188,heroH=available-tiles-info-28;
            hero.setBounds(0,top,leftW,heroH);eras.setBounds(0,top+heroH+14,leftW,tiles);
            classification.setBounds(0,top+heroH+tiles+28,leftW,info);lineage.setBounds(leftW+14,top,rightW,available);
            launch.setBounds(0,launchY,w,105);footer.setVisible(false);
            int right=w-22,opW=Math.min(330,w*29/100),setW=Math.min(230,w*21/100);
            age.setBounds(28,10,w-opW-setW-100,46);age.setFont(serif(w<980?26:34));build.setBounds(28,56,w-40,25);
            open.setBounds(right-opW,17,opW,53);setup.setBounds(right-opW-setW-22,17,setW,53);hint.setBounds(w/3,74,w*2/3-28,24);

        }else if(page!=null){footer.setVisible(true);page.setBounds(0,106,w,h-173);}
    }
    void openMinecraft(){
        open.setEnabled(false);open.setText("OPENING…");
        new SwingWorker<Void,Void>(){
            protected Void doInBackground()throws Exception{service.openMinecraft();return null;}
            protected void done(){open.setEnabled(true);open.setText("OPEN MINECRAFT  ↗");try{get();canon.event(service,"LOCAL","Requested official Minecraft launcher handoff; game start is not confirmed.");info("In the official launcher, select:\nMine Canon | "+LauncherService.NAMES[service.selected()]+"\n\nIf it is missing, use Worlds → Prepare profile first.\nSign in and press Play there.");}catch(Exception e){error(new Exception("Could not open Minecraft automatically.\nChoose the launcher executable in Settings, or open it manually.\n\n"+(e.getCause()==null?e.getMessage():e.getCause().getMessage())));}}
        }.execute();
    }

    void install(){
        String base=service.vanillaInstalled()?"":"First run Minecraft Java 1.20.1 once from the official launcher.\n\n";
        int ok=JOptionPane.showConfirmDialog(this,base+"The bundled Forge installer will open. Choose Install client.\nSet its installation folder to:\n"+service.minecraft()+"\n\nAfter it finishes, use Worlds → Prepare profile.\nOpen the installer now?","Forge 47.4.26 setup",JOptionPane.OK_CANCEL_OPTION);
        if(ok!=JOptionPane.OK_OPTION)return;
        canon.event(service,"LOCAL","Opening supplied Forge installer.");setup.setEnabled(false);setup.setText("Opening installer…");
        new SwingWorker<Integer,Void>(){protected Integer doInBackground()throws Exception{return service.startInstaller().waitFor();}protected void done(){setup.setEnabled(true);setup.setText("Set up Forge");try{int code=get();canon.event(service,"LOCAL","Forge installer exited with code "+code);runAudit();refresh();if(code!=0)info("Forge installer exited with code "+code+". Check forge-setup.log in "+service.home);else info(service.forgeInstalled()?"Forge profile detected. Next: Worlds → Prepare profile.":"Installer closed. Forge was not detected in the selected Minecraft folder.\nCheck Settings or complete the client installation.");}catch(Exception e){error(e);}}}.execute();
    }
    void prepare(){
        int ok=JOptionPane.showConfirmDialog(this,"Close the official Minecraft launcher before continuing.\n\nCreate or update Mine Canon | "+LauncherService.NAMES[service.selected()]+"?\nOther profiles are preserved and a backup is saved.","Prepare profile",JOptionPane.OK_CANCEL_OPTION);
        if(ok!=JOptionPane.OK_OPTION)return;
        try{service.prepareProfile(service.selected());canon.event(service,"LOCAL","Prepared official Minecraft profile: "+LauncherService.NAMES[service.selected()]);info("Profile ready: Mine Canon | "+LauncherService.NAMES[service.selected()]+"\n\nOpen Minecraft, choose this installation and press Play.\nMemory: "+service.memory()+" GB\nGame folder: "+service.instance(service.selected()));refresh();}catch(Exception e){error(e);}
    }
    JPanel panel(){JPanel p=new JPanel();p.setOpaque(false);return p;}
    JLabel label(String text,int size,Color color){JLabel l=new JLabel(text);l.setFont(sans(size));l.setForeground(color);return l;}
    JTextArea note(String text){JTextArea t=new JTextArea(text){@Override public Dimension getPreferredSize(){int w=getParent()!=null&&getParent().getWidth()>0?getParent().getWidth():700;FontMetrics fm=getFontMetrics(getFont()==null?sans(14):getFont());int lines=0;for(String part:getText().split("\\n",-1))lines+=Math.max(1,(int)Math.ceil((double)fm.stringWidth(part)/Math.max(100,w-40)));return new Dimension(w,(lines+1)*fm.getHeight());}};t.setEditable(false);t.setLineWrap(true);t.setWrapStyleWord(true);t.setOpaque(false);t.setForeground(MUTED);t.setFont(sans(14));t.setFocusable(false);return t;}
    JComponent worlds(){
        JPanel wrap=panel();wrap.setLayout(new BorderLayout(0,18));
        wrap.add(note("Separate local instances inspired by your Canon Field ages. These are launcher themes and folders; maps, creatures and gameplay mods are not bundled."),BorderLayout.NORTH);
        JPanel rows=panel();rows.setLayout(new GridLayout(6,1,0,8));
        for(int i=0;i<6;i++){final int n=i;JPanel row=new JPanel(new BorderLayout(20,0));row.setBackground(service.selected()==i?new Color(47,34,26):PANEL);row.setBorder(new EmptyBorder(10,18,10,18));
            JPanel text=panel();text.setLayout(new GridLayout(3,1));text.add(label(String.format("%02d   %s",i+1,LauncherService.NAMES[i]),20,TEXT));text.add(label(LauncherService.LORE[i],12,MUTED));text.add(label(CanonModel.AGE[i]+" · "+CanonModel.band(i),12,SAGE));row.add(text,BorderLayout.CENTER);
            ActionButton b=new ActionButton(service.selected()==i?"SELECTED":"Select age",service.selected()==i);b.setPreferredSize(new Dimension(135,40));b.addActionListener(e->select(n));row.add(b,BorderLayout.EAST);rows.add(row);
        }
        wrap.add(rows,BorderLayout.CENTER);JPanel actions=panel();actions.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));ActionButton p=new ActionButton("Prepare profile",true);p.setPreferredSize(new Dimension(190,48));p.addActionListener(e->prepare());actions.add(p);ActionButton f=new ActionButton("Open instance folder",false);f.setPreferredSize(new Dimension(250,48));f.addActionListener(e->{try{service.openFolder(service.ensureInstance(service.selected()));}catch(Exception ex){error(ex);}});actions.add(Box.createHorizontalStrut(12));actions.add(f);wrap.add(actions,BorderLayout.SOUTH);return wrap;
    }
    JComponent mods(){
        JPanel p=panel();p.setLayout(new BorderLayout(0,20));p.add(note("Local JAR files for "+LauncherService.NAMES[service.selected()]+". Use mods compatible with Minecraft 1.20.1 and Forge. A listed file is not a compatibility check."),BorderLayout.NORTH);
        DefaultListModel<String> model=new DefaultListModel<>();try{var paths=service.mods(service.selected());if(paths.isEmpty())model.addElement("No mods installed in this instance yet.");else for(Path file:paths)model.addElement(file.getFileName()+"   ·   "+(Files.size(file)/1024)+" KB");}catch(Exception e){model.addElement("Could not read mods: "+e.getMessage());}
        JList<String> list=new JList<>(model);list.setBackground(PANEL);list.setForeground(TEXT);list.setFont(sans(16));list.setFixedCellHeight(44);list.setBorder(new EmptyBorder(12,16,12,16));JScrollPane scroll=new JScrollPane(list);scroll.setBorder(new LineBorder(LINE));p.add(scroll,BorderLayout.CENTER);
        JPanel buttons=panel();buttons.setLayout(new FlowLayout(FlowLayout.LEFT,0,0));ActionButton b=new ActionButton("Open mods folder",true);b.setPreferredSize(new Dimension(205,48));b.addActionListener(e->{try{service.openFolder(service.ensureInstance(service.selected()).resolve("mods"));}catch(Exception ex){error(ex);}});buttons.add(b);buttons.add(Box.createHorizontalStrut(12));ActionButton refresh=new ActionButton("Refresh list",false);refresh.setPreferredSize(new Dimension(150,48));refresh.addActionListener(e->showPage("Mods"));buttons.add(refresh);p.add(buttons,BorderLayout.SOUTH);return p;
    }
    JComponent settings(){
        JPanel outer=panel();outer.setLayout(new BorderLayout());JPanel p=panel();p.setLayout(new GridBagLayout());GridBagConstraints c=new GridBagConstraints();c.gridx=0;c.gridy=0;c.weightx=1;c.fill=GridBagConstraints.HORIZONTAL;c.anchor=GridBagConstraints.NORTHWEST;c.insets=new Insets(0,0,14,0);
        JTextField mc=field(service.minecraft().toString()),exe=field(service.config.getProperty("launcher",""));
        p.add(label("Minecraft installation folder",15,TEXT),c);c.gridy++;p.add(pathRow(mc,true),c);c.gridy++;
        p.add(label("Minecraft launcher executable (optional; blank = automatic)",15,TEXT),c);c.gridy++;p.add(pathRow(exe,false),c);c.gridy++;
        p.add(label("Profile file",15,TEXT),c);c.gridy++;JComboBox<String> profile=new JComboBox<>(new String[]{"Auto-detect","Standard launcher","Microsoft Store launcher"});profile.setSelectedIndex(switch(service.config.getProperty("profiles","auto")){case "standard"->1;case "store"->2;default->0;});styleCombo(profile);p.add(profile,c);c.gridy++;
        p.add(label("Game memory allocation",15,TEXT),c);c.gridy++;JComboBox<Integer> ram=new JComboBox<>(new Integer[]{2,3,4,5,6,7,8,9,10,11,12,13,14,15,16});ram.setSelectedItem(service.memory());styleCombo(ram);p.add(ram,c);c.gridy++;
        p.add(note("Memory is applied when you prepare the selected profile. Leave enough RAM for Windows and other apps. Instance data: "+service.home+"\n\nYour name is launcher branding, not your Minecraft account. Sign-in stays in the official launcher."),c);c.gridy++;
        ActionButton save=new ActionButton("Save settings",true);save.setPreferredSize(new Dimension(200,48));save.addActionListener(e->{try{
            Path dir=Path.of(mc.getText().trim());if(!dir.isAbsolute()||!Files.isDirectory(dir))throw new Exception("Choose an existing absolute Minecraft directory.");
            String custom=exe.getText().trim();if(!custom.isEmpty()&&!Files.isRegularFile(Path.of(custom)))throw new Exception("Choose an existing launcher executable, or leave the field empty.");
            service.config.setProperty("minecraft",dir.toString());service.config.setProperty("launcher",custom);service.config.setProperty("ram",ram.getSelectedItem().toString());service.config.setProperty("profiles",new String[]{"auto","standard","store"}[profile.getSelectedIndex()]);service.save();refresh();info("Settings saved. Use Worlds → Prepare profile to apply memory changes.");
        }catch(Exception ex){error(ex);}});p.add(save,c);outer.add(p,BorderLayout.NORTH);JScrollPane scroll=new JScrollPane(outer);scroll.setBorder(null);scroll.setOpaque(false);scroll.getViewport().setOpaque(false);return scroll;
    }
    void styleCombo(JComboBox<?> box){box.setFont(sans(15));box.setBackground(PANEL);box.setForeground(TEXT);box.setPreferredSize(new Dimension(100,38));}
    JTextField field(String value){JTextField f=new JTextField(value);f.setFont(sans(14));f.setBackground(PANEL);f.setForeground(TEXT);f.setCaretColor(ORANGE);f.setBorder(BorderFactory.createCompoundBorder(new LineBorder(LINE),new EmptyBorder(9,10,9,10)));return f;}
    JPanel pathRow(JTextField field,boolean directory){JPanel p=panel();p.setLayout(new BorderLayout(12,0));p.add(field,BorderLayout.CENTER);ActionButton b=new ActionButton("Browse…",false);b.setPreferredSize(new Dimension(120,40));b.addActionListener(e->{JFileChooser f=new JFileChooser();if(directory)f.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);if(f.showOpenDialog(this)==JFileChooser.APPROVE_OPTION)field.setText(f.getSelectedFile().getAbsolutePath());});p.add(b,BorderLayout.EAST);return p;}
    static void quality(Graphics2D g){g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BICUBIC);}
    void drawArt(Graphics2D g,int w,int h,double focus){if(art==null){g.setColor(PANEL);g.fillRect(0,0,w,h);return;}double scale=Math.max((double)w/art.getWidth(),(double)h/art.getHeight());int dw=(int)(art.getWidth()*scale),dh=(int)(art.getHeight()*scale);g.drawImage(art,(int)((w-dw)*focus),(h-dh)/2,dw,dh,null);}
    final class Hero extends JPanel {Hero(){setOpaque(false);getAccessibleContext().setAccessibleName("Mine Canon voxel shrine landscape");}protected void paintComponent(Graphics gg){Graphics2D g=(Graphics2D)gg.create();quality(g);int w=getWidth(),h=getHeight();drawArt(g,w,h,.5);g.setPaint(new GradientPaint(0,0,new Color(0,0,0,130),w*.58f,0,new Color(0,0,0,0)));g.fillRect(0,0,w,h);int size=Math.min(94,Math.max(50,h/5));g.setColor(TEXT);g.setFont(serif(size));g.drawString("MINE",28,h/2-12);g.drawString("CANON",28,h/2+size-20);int lineY=h/2+size+3;g.setColor(ORANGE);g.fillRect(30,lineY,76,4);g.setColor(TEXT);g.setFont(new Font("Serif",Font.PLAIN,w<650?20:25));g.drawString("One field. Every age.",30,lineY+40);g.setColor(new Color(205,209,214));g.setFont(sans(10));g.drawString("C A N O N   H U N T E R S  &  M O N S T E R S",30,h-22);g.dispose();}}
    final class EraButton extends JButton {final int index;EraButton(int i){index=i;setText(LauncherService.NAMES[i]);setToolTipText("Select separate local instance: "+LauncherService.NAMES[i]);setBorderPainted(false);setContentAreaFilled(false);setFocusPainted(false);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));}protected void paintComponent(Graphics gg){Graphics2D g=(Graphics2D)gg.create();quality(g);int w=getWidth(),h=getHeight();if(eraArt==null)drawArt(g,w,h,index*.5);else{int cell=eraArt.getWidth()/3,sx=index*cell,sh=Math.min(eraArt.getHeight(),cell*h/w),sy=(eraArt.getHeight()-sh)/2;g.drawImage(eraArt,0,0,w,h,sx,sy,sx+cell,sy+sh,null);}g.setPaint(new GradientPaint(0,0,new Color(0,0,0,45),0,h,new Color(0,0,0,220)));g.fillRect(0,0,w,h);if(getModel().isRollover()){g.setColor(new Color(245,124,44,30));g.fillRect(0,0,w,h);}g.setFont(serif(w<240?18:22));g.setColor(ORANGE);g.drawString("0"+(index+1),12,h-20);g.setColor(TEXT);g.setFont(new Font("Serif",Font.PLAIN,w<240?14:19));g.drawString(LauncherService.NAMES[index],49,h-20);if(service.selected()==index){g.setColor(ORANGE);g.fillRect(0,h-4,w,4);}if(hasFocus()){g.setColor(TEXT);g.drawRect(2,2,w-5,h-5);}g.dispose();}}
    final class Sidebar extends JPanel {
        final JButton[] nav=new JButton[6];Sidebar(){setLayout(null);setBackground(BG);String[] names={"Home","Worlds","Mods","Settings","Verification","Boot log"};for(int i=0;i<6;i++){final String name=names[i];final int ix=i;nav[i]=new JButton(name){protected void paintComponent(Graphics gg){Graphics2D g=(Graphics2D)gg.create();quality(g);if(current.equals(name)){g.setColor(new Color(57,37,24));g.fillRoundRect(0,0,getWidth(),getHeight(),9,9);g.setColor(ORANGE);g.fillRect(0,0,4,getHeight());}else if(getModel().isRollover()){g.setColor(PANEL);g.fillRect(0,0,getWidth(),getHeight());}g.setColor(current.equals(name)?ORANGE:MUTED);icon(g,ix,30,19);g.setFont(sans(17));g.setColor(current.equals(name)?TEXT:MUTED);g.drawString(name,79,36);if(hasFocus()){g.setColor(ORANGE);g.drawRect(5,3,getWidth()-9,getHeight()-7);}g.dispose();}};nav[i].setContentAreaFilled(false);nav[i].setBorderPainted(false);nav[i].setFocusPainted(false);nav[i].setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));nav[i].addActionListener(e->showPage(name));add(nav[i]);}}
        public void doLayout(){for(int i=0;i<6;i++)nav[i].setBounds(4,210+i*61,getWidth()-15,56);}
        protected void paintComponent(Graphics gg){super.paintComponent(gg);Graphics2D g=(Graphics2D)gg.create();quality(g);int w=getWidth();g.setColor(LINE);g.drawLine(w-1,0,w-1,getHeight());int x=w/2-28;g.setColor(ORANGE);Path2D p=new Path2D.Double();p.moveTo(x,40);p.lineTo(x+27,64);p.lineTo(x+55,40);p.lineTo(x+55,103);p.lineTo(x+42,103);p.lineTo(x+42,70);p.lineTo(x+27,84);p.lineTo(x+13,70);p.lineTo(x+13,94);p.lineTo(x,107);p.closePath();g.fill(p);g.setColor(TEXT);g.setFont(serif(w<220?22:25));center(g,"MINE CANON",w,139);g.setFont(sans(10));g.setColor(MUTED);center(g,"W O R L D   L A U N C H E R",w,164);g.setColor(new Color(78,38,17));g.fillOval(23,getHeight()-97,55,55);g.setColor(ORANGE);g.drawOval(23,getHeight()-97,55,55);g.setColor(TEXT);g.setFont(serif(21));g.drawString("JM",33,getHeight()-61);g.setFont(sans(14));g.drawString("John Matukutire",90,getHeight()-64);g.dispose();}
    }
    static void center(Graphics2D g,String text,int w,int y){g.drawString(text,(w-g.getFontMetrics().stringWidth(text))/2,y);}
    static void icon(Graphics2D g,int type,int x,int y){g.setStroke(new BasicStroke(2));if(type==0){Polygon p=new Polygon(new int[]{x,x+12,x+24,x+20,x+20,x+14,x+14,x+9,x+9,x+4,x+4},new int[]{y+10,y,y+10,y+10,y+24,y+24,y+16,y+16,y+24,y+24,y+10},11);g.fill(p);}else if(type==1){g.drawOval(x,y,24,24);g.drawOval(x+7,y,10,24);g.drawLine(x,y+12,x+24,y+12);}else if(type==2){g.drawRoundRect(x+2,y+2,20,20,3,3);g.drawLine(x+12,y-2,x+12,y+26);g.drawLine(x-2,y+12,x+26,y+12);}else if(type==4){g.drawPolygon(new int[]{x+2,x+12,x+22,x+19,x+12,x+5},new int[]{y+3,y,y+3,y+17,y+25,y+17},6);g.drawLine(x+6,y+11,x+10,y+15);g.drawLine(x+10,y+15,x+18,y+7);}else if(type==5){g.drawRect(x+3,y,18,25);g.drawLine(x+7,y+7,x+17,y+7);g.drawLine(x+7,y+12,x+17,y+12);g.drawLine(x+7,y+17,x+14,y+17);}else{g.drawOval(x+3,y+3,18,18);g.drawOval(x+9,y+9,6,6);for(int i=0;i<8;i++){double a=i*Math.PI/4;g.drawLine(x+12+(int)(10*Math.cos(a)),y+12+(int)(10*Math.sin(a)),x+12+(int)(15*Math.cos(a)),y+12+(int)(15*Math.sin(a)));}}}
    static class ActionButton extends JButton {final boolean accent;ActionButton(String text,boolean accent){super(text);this.accent=accent;setFont(sans(18));setForeground(accent?Color.WHITE:TEXT);setContentAreaFilled(false);setBorderPainted(false);setFocusPainted(false);setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));setMargin(new Insets(10,12,10,12));}protected void paintComponent(Graphics gg){Graphics2D g=(Graphics2D)gg.create();quality(g);g.setColor(!isEnabled()?LINE:accent?(getModel().isRollover()?ORANGE.brighter():ORANGE):(getModel().isRollover()?new Color(44,53,60):new Color(28,35,40)));g.fillRoundRect(0,0,getWidth(),getHeight(),8,8);g.setColor(accent?ORANGE:LINE.brighter());g.drawRoundRect(0,0,getWidth()-1,getHeight()-1,8,8);if(hasFocus()){g.setColor(TEXT);g.drawRoundRect(3,3,getWidth()-7,getHeight()-7,6,6);}g.dispose();super.paintComponent(gg);}}
}
