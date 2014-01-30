/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.kthfsdashboard.provision;

/**
 * Enum which specifies the different types of providers we currently support (and future)
 * @author Alberto Lorente Leal <albll@kth.se>
 */
public enum ProviderType {
    AWS_EC2("aws-ec2"),
    OPENSTACK("openstack-nova"),
    RACKSPACE("rackspace"),
    BAREMETAL("baremetal");
    
    private final String text;
    
    private ProviderType(final String text){
        this.text=text;
    }
    
    public static ProviderType fromString(String text){
        if(text!=null){
            for(ProviderType value: ProviderType.values()){
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
