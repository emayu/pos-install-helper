
package dev.yaque.pos.installhelper;

/**
 *
 * @author yaque
 */
public class ProgressBar {
    
    private int progress = 0;
    private StringBuilder builder = new StringBuilder(200);
    private final char[] animatedChars = {'|', '/', '-', '\\'};
    
    public void setProgress(int progress){
        if(progress >= 0 && progress <= 100){
            this.progress = progress;
        }
    }
    
    private String generateBar(){
        builder.setLength(0);
        builder.append(animatedChars[progress % animatedChars.length]);
        builder.append('|');
        builder.append("====================================================================================================", 0, progress);
        builder.append("                                                                                                    ", progress, 100);
        builder.append("| ");
        builder.append(progress);
        builder.append("%\r");
        return builder.toString();
                
    }
    
    
    public void print(){
        if(progress == 100){
            System.out.println(generateBar());
        }else{
            System.out.print(generateBar());
        }
    }
    
    public static void main(String[] args) throws InterruptedException {
        ProgressBar bar =  new ProgressBar();
        for(int i =0; i <= 100; i++){
            bar.setProgress(i);
            bar.print();
            if(i == 0){ Thread.sleep(1500); }
            Thread.sleep(100);
        }
    }
}
