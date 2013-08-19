/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

/**
 *
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public enum Provider {
    AWS_EC2("aws-ec2"),
    OPENSTACK("openstack-nova"),
    RACKSPACE("rackspace");
    
    private final String text;
    
    private Provider(final String text){
        this.text=text;
    }
    
    public static Provider fromString(String text){
        if(text!=null){
            for(Provider value: Provider.values()){
                if(text.equals(value.toString())){
                    return value;
                }
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return text;
    }
    
    
}
