import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;

/** The Mods page: dependency-aware sorter with category stripes, filter, search, diagnostics and enable/disable. */
final class ModsPanel extends JPanel {
    private static final Color BG=LauncherUI.BG,PANEL=LauncherUI.PANEL,TEXT=LauncherUI.TEXT,MUTED=LauncherUI.MUTED,ORANGE=LauncherUI.ORANGE,LINE=LauncherUI.LINE,SAGE=LauncherUI.SAGE;
    private static final Color ERR=new Color(224,90,90),WARN=new Color(245,190,80);

    private final LauncherUI ui;private final int age;private final Path dir;
    private ModSorter.Result result=new ModSorter.Result();
    private ModSorter.Mode mode=ModSorter.Mode.LOAD_PLAN;
    private ModSorter.Category filter=null;
    private final DefaultListModel<ModSorter.Mod> model=new DefaultListModel<>();
    private final JList<ModSorter.Mod> list=new JList<>(model);
    private final DefaultListModel<ModSorter.Issue> issueModel=new DefaultListModel<>();
    private final JList<ModSorter.Issue> issueList=new JList<>(issueModel);
    private final JTextField search=new JTextField(){@Override protected void paintComponent(Graphics g){super.paintComponent(g);if(getText().isEmpty()){Graphics2D h=(Graphics2D)g.create();LauncherUI.quality(h);h.setColor(MUTED.darker());h.setFont(getFont());h.drawString("Search name, id or file…",12,(getHeight()+h.getFontMetrics().getAscent()-h.getFontMetrics().getDescent())/2);h.dispose();}}};
    private final LauncherUI.ActionButton sortBtn=new LauncherUI.ActionButton("",false),filterBtn=new LauncherUI.ActionButton("",false),toggleBtn=new LauncherUI.ActionButton("Disable",false);
    private final Details details=new Details();
    private final JLabel summary=new JLabel();

    static JComponent build(LauncherUI ui){return new ModsPanel(ui);}

    private ModsPanel(LauncherUI ui){
        this.ui=ui;this.age=ui.service.selected();
        setOpaque(false);setLayout(new BorderLayout(0,12));
        Path d;try{d=ui.service.ensureInstance(age).resolve("mods");}catch(Exception e){d=ui.service.instance(age).resolve("mods");}dir=d;

        JLabel note=ui.label("Mods for "+LauncherService.NAMES[age]+". LOAD PLAN puts every dependency ahead of the mods that need it. Read from each jar's mods.toml - declared dependencies only; this does not run the game.",13,MUTED);
        // toolbar
        JPanel bar=new JPanel(new GridBagLayout());bar.setOpaque(false);
        search.setFont(LauncherUI.sans(15));search.setBackground(new Color(8,12,15));search.setForeground(TEXT);search.setCaretColor(TEXT);search.setBorder(new CompoundBorder(new LineBorder(LINE.brighter()),new EmptyBorder(6,10,6,10)));
        search.setToolTipText("Search by name, id or file");
        search.getDocument().addDocumentListener(new javax.swing.event.DocumentListener(){public void insertUpdate(javax.swing.event.DocumentEvent e){refill();}public void removeUpdate(javax.swing.event.DocumentEvent e){refill();}public void changedUpdate(javax.swing.event.DocumentEvent e){refill();}});
        sortBtn.setFont(LauncherUI.sans(14));filterBtn.setFont(LauncherUI.sans(14));toggleBtn.setFont(LauncherUI.sans(14));
        sortBtn.addActionListener(e->{mode=mode.next();refill();});
        filterBtn.addActionListener(e->{ModSorter.Category[] c=ModSorter.Category.values();int i=filter==null?0:filter.ordinal()+1;filter=i>=c.length?null:c[i];refill();});
        toggleBtn.addActionListener(e->toggleSelected());
        GridBagConstraints g=new GridBagConstraints();g.fill=GridBagConstraints.HORIZONTAL;g.insets=new Insets(0,0,0,10);g.ipady=6;
        g.gridx=0;g.weightx=1;bar.add(search,g);g.gridx=1;g.weightx=0;g.ipadx=18;bar.add(sortBtn,g);g.gridx=2;bar.add(filterBtn,g);g.gridx=3;g.insets=new Insets(0,0,0,0);bar.add(toggleBtn,g);
        JPanel north=new JPanel(new BorderLayout(0,10));north.setOpaque(false);north.add(note,BorderLayout.NORTH);north.add(bar,BorderLayout.CENTER);
        add(north,BorderLayout.NORTH);

        // list
        list.setBackground(PANEL);list.setFixedCellHeight(54);list.setCellRenderer(new ModCell());list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        list.addListSelectionListener(e->{if(!e.getValueIsAdjusting())showDetails();});
        JScrollPane ls=new JScrollPane(list);ls.setBorder(new LineBorder(LINE));ls.getViewport().setBackground(PANEL);

        issueList.setBackground(PANEL);issueList.setCellRenderer(new IssueCell());issueList.setFixedCellHeight(-1);issueList.setSelectionBackground(PANEL);issueList.setFocusable(false);
        JScrollPane is=new JScrollPane(issueList);is.setBorder(new LineBorder(LINE));is.getViewport().setBackground(PANEL);is.setPreferredSize(new Dimension(100,165));issueList.addComponentListener(new java.awt.event.ComponentAdapter(){@Override public void componentResized(java.awt.event.ComponentEvent e){issueList.setFixedCellHeight(-1);issueList.revalidate();issueList.repaint();}});
        JPanel issuePane=new JPanel(new BorderLayout(0,6));issuePane.setOpaque(false);summary.setFont(LauncherUI.sans(13));summary.setForeground(MUTED);issuePane.add(summary,BorderLayout.NORTH);issuePane.add(is,BorderLayout.CENTER);

        JPanel left=new JPanel(new BorderLayout(0,10));left.setOpaque(false);left.add(ls,BorderLayout.CENTER);left.add(issuePane,BorderLayout.SOUTH);
        JScrollPane ds=new JScrollPane(details);ds.setBorder(new LineBorder(LINE));ds.getViewport().setBackground(PANEL);ds.setPreferredSize(new Dimension(330,100));ds.getVerticalScrollBar().setUnitIncrement(14);
        JSplitPane split=new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,left,ds);split.setOpaque(false);split.setBorder(null);split.setResizeWeight(1.0);split.setDividerSize(10);split.setContinuousLayout(true);
        add(split,BorderLayout.CENTER);

        // bottom buttons
        JPanel buttons=new JPanel(new FlowLayout(FlowLayout.LEFT,0,0));buttons.setOpaque(false);
        LauncherUI.ActionButton open=new LauncherUI.ActionButton("Open mods folder",true);open.setPreferredSize(new Dimension(205,46));open.addActionListener(e->{try{ui.service.openFolder(dir);}catch(Exception ex){ui.error(ex);}});
        LauncherUI.ActionButton refresh=new LauncherUI.ActionButton("Rescan",false);refresh.setPreferredSize(new Dimension(130,46));refresh.addActionListener(e->rescan());
        LauncherUI.ActionButton export=new LauncherUI.ActionButton("Export load plan",false);export.setPreferredSize(new Dimension(200,46));export.addActionListener(e->export());
        buttons.add(open);buttons.add(Box.createHorizontalStrut(12));buttons.add(refresh);buttons.add(Box.createHorizontalStrut(12));buttons.add(export);
        add(buttons,BorderLayout.SOUTH);

        rescan();
    }

    private void rescan(){
        ModSorter.Mod keep=list.getSelectedValue();String keepFile=keep==null?null:keep.fileName;
        try{result=ModSorter.scan(dir);}catch(Exception e){result=new ModSorter.Result();result.issues.add(new ModSorter.Issue(ModSorter.Severity.ERROR,"scan","Could not read the mods folder: "+e.getMessage()));}
        ui.canon.event(ui.service,"LOCAL","Mods scanned: "+result.mods.size()+" file(s), "+result.count(ModSorter.Severity.ERROR)+" error(s), "+result.count(ModSorter.Severity.WARN)+" warning(s).");
        refill();
        if(keepFile!=null)for(int i=0;i<model.size();i++)if(model.get(i).fileName.equals(keepFile)){list.setSelectedIndex(i);break;}
        if(list.getSelectedIndex()<0&&model.size()>0)list.setSelectedIndex(0);
        showDetails();
    }

    private void refill(){
        ModSorter.Mod sel=list.getSelectedValue();
        String q=search.getText().strip().toLowerCase(Locale.ROOT);
        model.clear();
        for(ModSorter.Mod m:ModSorter.sorted(result.mods,mode)){
            if(filter!=null&&m.category!=filter)continue;
            if(!q.isEmpty()&&!(m.name.toLowerCase(Locale.ROOT).contains(q)||m.id.toLowerCase(Locale.ROOT).contains(q)||m.fileName.toLowerCase(Locale.ROOT).contains(q)))continue;
            model.addElement(m);
        }
        if(sel!=null)for(int i=0;i<model.size();i++)if(model.get(i)==sel){list.setSelectedIndex(i);break;}
        sortBtn.setText("Sort: "+mode.label);filterBtn.setText("Show: "+(filter==null?"All":filter.label));
        issueModel.clear();
        List<ModSorter.Issue> is=new ArrayList<>(result.issues);is.sort(Comparator.comparing(ModSorter.Issue::severity));for(var i:is)issueModel.addElement(i);
        long en=result.mods.stream().filter(m->m.enabled).count();
        summary.setText(model.size()+" shown  ·  "+en+" enabled of "+result.mods.size()+"  ·  "+result.count(ModSorter.Severity.ERROR)+" error  ·  "+result.count(ModSorter.Severity.WARN)+" warning   -   diagnostics");
        if(result.mods.isEmpty()){model.clear();}
        revalidate();repaint();
    }

    private void showDetails(){
        ModSorter.Mod m=list.getSelectedValue();details.show(m,result);
        toggleBtn.setText(m==null||m.enabled?"Disable":"Enable");toggleBtn.setEnabled(m!=null);
    }

    private void toggleSelected(){
        ModSorter.Mod m=list.getSelectedValue();if(m==null)return;
        try{Path np=ModSorter.toggle(m);ui.canon.event(ui.service,"LOCAL",(m.enabled?"Disabled ":"Enabled ")+m.fileName+" -> "+np.getFileName());
            rescan();
            for(int i=0;i<model.size();i++)if(model.get(i).fileName.equals(np.getFileName().toString())){list.setSelectedIndex(i);break;}
        }catch(Exception e){ui.error(e);}
    }

    private void export(){
        try{Files.createDirectories(dir.getParent());Path out=dir.getParent().resolve("minecanon-load-plan.txt");
            Files.writeString(out,ModSorter.exportPlan(result,LauncherService.NAMES[age]));
            ui.canon.event(ui.service,"LOCAL","Load plan exported: "+out);ui.info("Load plan written to:\n"+out+"\n\nForge 1.20.1 loads the top-level mods folder itself, so this is a reference order, not a file rename.");
        }catch(Exception e){ui.error(e);}
    }

    // ---------- renderers ----------
    private final class ModCell extends JComponent implements ListCellRenderer<ModSorter.Mod> {
        ModSorter.Mod m;boolean sel;int index;
        public Component getListCellRendererComponent(JList<? extends ModSorter.Mod> l,ModSorter.Mod v,int i,boolean s,boolean f){m=v;sel=s;index=i;return this;}
        @Override public Dimension getPreferredSize(){return new Dimension(200,54);}
        @Override protected void paintComponent(Graphics gg){
            Graphics2D g=(Graphics2D)gg.create();LauncherUI.quality(g);int w=getWidth(),h=getHeight();
            g.setColor(sel?new Color(52,36,26):PANEL);g.fillRect(0,0,w,h);
            g.setColor(LINE);g.drawLine(0,h-1,w,h-1);
            Color cat=new Color(m.category.rgb);float a=m.enabled?1f:0.35f;
            g.setColor(new Color(cat.getRed(),cat.getGreen(),cat.getBlue(),(int)(255*a)));g.fillRoundRect(12,9,5,h-18,4,4);
            Color tc=m.enabled?TEXT:MUTED.darker();
            g.setFont(LauncherUI.sans(16).deriveFont(Font.BOLD));g.setColor(tc);
            String name=m.name+(m.enabled?"":"   (disabled)");FontMetrics fm=g.getFontMetrics();
            int right=w-16;String tag=m.enabled&&m.readable()?"#"+(m.plan+1):"";
            g.setFont(LauncherUI.sans(14));FontMetrics sm=g.getFontMetrics();int tagW=tag.isEmpty()?0:sm.stringWidth(tag);
            String catL=m.category.label;g.setFont(LauncherUI.sans(13));int catW=g.getFontMetrics().stringWidth(catL);
            int maxName=Math.max(60,right-30-catW-tagW-30);
            g.setFont(LauncherUI.sans(16).deriveFont(Font.BOLD));g.setColor(tc);g.drawString(clip(name,fm,maxName),28,23);
            g.setFont(LauncherUI.sans(12));g.setColor(MUTED);
            String sub=(m.readable()?m.id+"  ·  v"+m.version+"  ·  ":"")+m.fileName+"  ·  "+ModSorter.human(m.size);
            g.drawString(clip(sub,g.getFontMetrics(),right-40-Math.max(catW,tagW)),28,42);
            g.setFont(LauncherUI.sans(13));g.setColor(new Color(cat.getRed(),cat.getGreen(),cat.getBlue(),(int)(255*a)));g.drawString(catL,right-catW,23);
            if(!tag.isEmpty()){g.setFont(LauncherUI.sans(14));g.setColor(sel?ORANGE:MUTED);g.drawString(tag,right-tagW,42);}
            if(m.readable()&&!m.dependents.isEmpty()&&m.enabled){g.setFont(LauncherUI.sans(12));g.setColor(SAGE);String d="needed by "+m.dependents.size();g.drawString(d,right-tagW-g.getFontMetrics().stringWidth(d)-14,42);}
            g.dispose();
        }
    }
    static String clip(String s,FontMetrics fm,int max){if(fm.stringWidth(s)<=max)return s;while(s.length()>1&&fm.stringWidth(s+"…")>max)s=s.substring(0,s.length()-1);return s+"…";}

    private final class IssueCell extends JComponent implements ListCellRenderer<ModSorter.Issue> {
        ModSorter.Issue i;int width=560;
        public Component getListCellRendererComponent(JList<? extends ModSorter.Issue> l,ModSorter.Issue v,int idx,boolean s,boolean f){i=v;width=Math.max(300,l.getWidth()-4);return this;}
        private java.util.List<String> lines(FontMetrics fm){java.util.List<String> out=new ArrayList<>();StringBuilder cur=new StringBuilder();for(String w:i.text().split(" ")){if(fm.stringWidth(cur+" "+w)>width-40&&cur.length()>0){out.add(cur.toString());cur.setLength(0);}if(cur.length()>0)cur.append(' ');cur.append(w);}if(cur.length()>0)out.add(cur.toString());return out;}
        @Override public Dimension getPreferredSize(){width=Math.max(300,issueList.getWidth()>0?issueList.getWidth()-4:560);FontMetrics fm=getFontMetrics(LauncherUI.sans(13));return new Dimension(width,lines(fm).size()*17+10);}
        @Override protected void paintComponent(Graphics gg){
            Graphics2D g=(Graphics2D)gg.create();LauncherUI.quality(g);g.setColor(PANEL);g.fillRect(0,0,getWidth(),getHeight());
            Color c=i.severity()==ModSorter.Severity.ERROR?ERR:i.severity()==ModSorter.Severity.WARN?WARN:SAGE;
            g.setColor(c);g.fillOval(12,9,9,9);g.setFont(LauncherUI.sans(13));g.setColor(TEXT);FontMetrics fm=g.getFontMetrics();int y=16;for(String s:lines(fm)){g.drawString(s,30,y);y+=17;}g.dispose();
        }
    }

    private final class Details extends JComponent {
        ModSorter.Mod m;ModSorter.Result r;int need=300;
        void show(ModSorter.Mod mod,ModSorter.Result res){m=mod;r=res;revalidate();repaint();}
        @Override public Dimension getPreferredSize(){return new Dimension(300,Math.max(need,200));}
        private java.util.List<String> wrap(String s,FontMetrics fm,int width){java.util.List<String> out=new ArrayList<>();StringBuilder cur=new StringBuilder();for(String w:s.split(" ")){if(fm.stringWidth(cur+" "+w)>width&&cur.length()>0){out.add(cur.toString());cur.setLength(0);}if(cur.length()>0)cur.append(' ');cur.append(w);}if(cur.length()>0)out.add(cur.toString());return out;}
        @Override protected void paintComponent(Graphics gg){
            Graphics2D g=(Graphics2D)gg.create();LauncherUI.quality(g);int w=getWidth();g.setColor(PANEL);g.fillRect(0,0,w,getHeight());
            int x=18,y=34,tw=w-36;
            if(m==null){g.setFont(LauncherUI.sans(18).deriveFont(Font.BOLD));g.setColor(TEXT);g.drawString("Select a mod",x,y);y+=28;g.setFont(LauncherUI.sans(13));g.setColor(MUTED);for(String s:wrap("LOAD PLAN lists every dependency before the mods that need it. Click a row for requirements and dependents. Disabling renames the jar to .jar.disabled - nothing is deleted.",g.getFontMetrics(),tw)){g.drawString(s,x,y);y+=18;}
                y+=14;Map<ModSorter.Category,Integer> n=new EnumMap<>(ModSorter.Category.class);if(r!=null)for(var q:r.mods)n.merge(q.category,1,Integer::sum);
                int max=n.values().stream().max(Integer::compare).orElse(1);
                for(var c:ModSorter.Category.values()){int v=n.getOrDefault(c,0);if(v==0)continue;g.setColor(new Color(c.rgb));g.fillRect(x,y-11,10,10);g.setFont(LauncherUI.sans(13));g.setColor(TEXT);g.drawString(c.label+"  "+v,x+18,y);g.setColor(new Color(c.rgb));g.fillRect(x+130,y-10,Math.max(6,(tw-130)*v/max),8);y+=22;}
                need=y+16;g.dispose();return;}
            g.setFont(LauncherUI.sans(19).deriveFont(Font.BOLD));g.setColor(TEXT);g.drawString(clip(m.name,g.getFontMetrics(),tw),x,y);y+=22;
            g.setFont(LauncherUI.sans(13));g.setColor(MUTED);g.drawString(clip(m.readable()?m.id+" · v"+m.version:m.fileName,g.getFontMetrics(),tw),x,y);y+=24;
            Color c=new Color(m.category.rgb);g.setColor(c);g.fillRect(x,y-11,10,10);g.setColor(c);g.drawString(m.category.label+(m.readable()&&m.enabled?"  ·  plan #"+(m.plan+1):"")+"  ·  "+ModSorter.human(m.size)+(m.enabled?"":"  ·  disabled"),x+18,y);y+=26;
            if(m.readable()){
                y=list(g,"Requires ("+m.requires.size()+")",m.requires,ORANGE,x,y,tw);
                y=list(g,"Needed by ("+m.dependents.size()+")",m.dependents,SAGE,x,y,tw);
                if(!m.optional.isEmpty())y=list(g,"Optional ("+m.optional.size()+")",m.optional,MUTED,x,y,tw);
                if(!m.bundled.isEmpty())y=list(g,"Bundles (jar-in-jar)",new ArrayList<>(m.bundled.keySet()),new Color(111,168,220),x,y,tw);
                java.util.List<String> bad=new ArrayList<>();for(var i:r.issues)if(i.modId().equals(m.id)&&i.severity()!=ModSorter.Severity.INFO)bad.add(i.text());
                if(!bad.isEmpty()){g.setFont(LauncherUI.sans(14).deriveFont(Font.BOLD));g.setColor(ERR);g.drawString("Problems",x,y);y+=18;g.setFont(LauncherUI.sans(13));g.setColor(TEXT);for(String b:bad)for(String s:wrap(b,g.getFontMetrics(),tw)){g.drawString(s,x,y);y+=17;}y+=8;}
            }
            g.setFont(LauncherUI.sans(12));g.setColor(MUTED);g.drawString("File: "+clip(m.fileName,g.getFontMetrics(),tw-30),x,y);y+=22;
            g.setFont(LauncherUI.sans(13));g.setColor(MUTED);for(String s:wrap(m.description.isEmpty()?"No description in mods.toml.":m.description,g.getFontMetrics(),tw)){g.drawString(s,x,y);y+=17;}
            need=y+16;g.dispose();
        }
        private int list(Graphics2D g,String head,java.util.List<String> ids,Color c,int x,int y,int tw){
            g.setFont(LauncherUI.sans(14).deriveFont(Font.BOLD));g.setColor(c);g.drawString(head,x,y);y+=18;g.setFont(LauncherUI.sans(13));g.setColor(TEXT);
            if(ids.isEmpty()){g.setColor(MUTED);g.drawString("none",x,y);return y+24;}
            for(String s:wrap(String.join(", ",ids),g.getFontMetrics(),tw)){g.drawString(s,x,y);y+=17;}return y+10;
        }
    }
}
