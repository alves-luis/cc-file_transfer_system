package transferecc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * This class holds all the details for each session
 * @author alvesluis
 */
public class Configuration {
    
    private List<String> ips;
    private String fname;
    
    public Configuration(String filename) {
        this.fname = filename;
        this.ips = new ArrayList<>();
        try {
            BufferedReader reader = new BufferedReader(new FileReader(filename));
            String line;
            while((line = reader.readLine()) != null)
                ips.add(line);       
        }
        catch (FileNotFoundException e) {
            File config = new File(filename);
            try {
                config.createNewFile();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * @return Returns an array with all the IPs in the imported file
     */
    public String[] getIPS() {
        String[] result = new String[ips.size()];
        for(int i = 0; i < ips.size(); i++)
            result[i] = ips.get(i);
        return result;
    }

    /**
     * Adds a new IP to the file which holds all the destination IPs
     * @param ip String representation of the ip to be added to the configuration file
     */
    public void addIP(String ip) {
        try {
            PrintWriter writer = new PrintWriter(new FileWriter(fname,true),true);
            writer.println(ip);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Returns the String representation of the i-th IP
     * @param i Index of IP
     * @return String representation of the IP, or NULL if invalid index
     */
    public String getIP(int i) {
        if (i < this.ips.size())
            return this.ips.get(i);
        else
            return null;
    }
    
    public int numIps(){
        return this.ips.size();
    }
}
