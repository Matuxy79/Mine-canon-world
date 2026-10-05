import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.file.*;
import javax.imageio.ImageIO;

public class MineCanonLauncher {
    static Path bundle()throws Exception {Path p=Path.of(MineCanonLauncher.class.getProtectionDomain().getCodeSource().getLocation().toURI());return Files.isDirectory(p)?p.getParent():p.getParent();}
    static void layout(Component c){if(c instanceof Container n){n.doLayout();for(Component child:n.getComponents())layout(child);}}
    public static void main(String[] args)throws Exception {
        if(args.length>0&&args[0].equals("--render")){
            SwingUtilities.invokeAndWait(()->{try{LauncherService service=new LauncherService(LauncherService.defaultHome(),bundle());LauncherUI ui=new LauncherUI(service);if(args.length>2)ui.showPage(args[2]);int w=args.length>3?Integer.parseInt(args[3]):1536,h=args.length>4?Integer.parseInt(args[4]):1024;ui.setSize(w,h);layout(ui);layout(ui);BufferedImage image=new BufferedImage(w,h,BufferedImage.TYPE_INT_RGB);Graphics2D g=image.createGraphics();ui.printAll(g);g.dispose();ImageIO.write(image,"png",Path.of(args[1]).toFile());}catch(Exception e){throw new RuntimeException(e);}});return;
        }
        if(args.length>0&&args[0].equals("--quick")){
            SwingUtilities.invokeLater(()->{try{
                UIManager.put("Panel.background",LauncherUI.PANEL);UIManager.put("OptionPane.background",LauncherUI.PANEL);UIManager.put("OptionPane.messageForeground",LauncherUI.TEXT);UIManager.put("OptionPane.messageFont",LauncherUI.sans(14));
                LauncherService service=new LauncherService(LauncherService.defaultHome(),bundle());
                LauncherUI ui=new LauncherUI(service);ui.setSize(900,680);layout(ui);ui.showPage("Mods");
                JFrame frame=new JFrame("MineCanon Quick — John Matukutire");frame.setAlwaysOnTop(true);frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);frame.setContentPane(ui);frame.setSize(900,680);frame.setMinimumSize(new Dimension(760,620));frame.setLocationRelativeTo(null);
                try{frame.setIconImages(java.util.List.of(MineCanonIcon.render(64),MineCanonIcon.render(32)));}catch(Exception ignored){}
                frame.setVisible(true);
            }catch(Exception e){JOptionPane.showMessageDialog(null,"Could not start MineCanon Quick:\n"+e,"MineCanon",JOptionPane.ERROR_MESSAGE);}});
            return;
        }
        SwingUtilities.invokeLater(()->{try{
            UIManager.put("Panel.background",LauncherUI.PANEL);UIManager.put("OptionPane.background",LauncherUI.PANEL);UIManager.put("OptionPane.messageForeground",LauncherUI.TEXT);UIManager.put("OptionPane.messageFont",LauncherUI.sans(14));
            LauncherService service=new LauncherService(LauncherService.defaultHome(),bundle());
            LauncherUI ui=new LauncherUI(service);JFrame frame=new JFrame("Mine Canon — John Matukutire");frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);frame.setContentPane(ui);frame.setSize(1380,920);frame.setMinimumSize(new Dimension(1120,820));frame.setLocationRelativeTo(null);try{frame.setIconImages(java.util.List.of(MineCanonIcon.render(64),MineCanonIcon.render(32)));}catch(Exception ignored){}frame.setVisible(true);
            frame.addWindowFocusListener(new java.awt.event.WindowAdapter(){public void windowGainedFocus(java.awt.event.WindowEvent e){ui.refresh();}});
            if(!service.forgeInstalled()&&!"false".equals(service.config.getProperty("autoopen","true")))SwingUtilities.invokeLater(()->ui.offerSetup());
        }catch(Exception e){JOptionPane.showMessageDialog(null,"Could not start Mine Canon:\n"+e,"Mine Canon",JOptionPane.ERROR_MESSAGE);}});
    }
}
