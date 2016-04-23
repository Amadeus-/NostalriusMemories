package bunnyEmu.main.handlers;

import java.util.ArrayList;

import bunnyEmu.main.Server;
import bunnyEmu.main.entities.Client;
import bunnyEmu.main.entities.Realm;
import bunnyEmu.main.entities.packet.AuthPacket;
import bunnyEmu.main.enums.ClientVersion;

public class RealmHandler {
    private static ArrayList<Realm> realms = new ArrayList<Realm>(10);

    /**
     * @return All launched realms
     */
    public static ArrayList<Realm> getRealms(){
        return realms;
    }

    /**
     * @return The realmlist packet, version dependable
     */
    public static AuthPacket getRealmList(){
        short size = 0;
        for (int i = 0; i < realms.size(); i++) 
            size += realms.get(i).getSize();
        
        AuthPacket realmPacket = new AuthPacket((short) (1 + 2 + 4 + 1 + size + 2));
        
        byte REALM_LIST = 0x10;
        realmPacket.put(REALM_LIST);              // Header
        realmPacket.putShort((short) (size + 2 + 5));               // size to read
        realmPacket.putInt(0);                     // unknown
        realmPacket.put((byte) realms.size());       // Realm count [Vanilla]
       
        // all realms
        for (Realm realm : realms) {
            // Vanilla stuff
            byte AmountOfCharacters = 1;
            byte timezone = 1;
            byte realmID = 0;
            realmPacket.putInt(0x1); // Icon
            realmPacket.put((byte) realm.flags);
            realmPacket.putString(realm.name);      // Name
            realmPacket.putString(realm.address);   // Address
            realmPacket.putFloat(realm.population);      // Population
            realmPacket.put(AmountOfCharacters); // char count
            realmPacket.put(timezone);        // timezone
            realmPacket.put(realmID);        // realmID
        }
        
        realmPacket.putShort((short) 0x0200);
        realmPacket.wrap();
        return realmPacket;
    }

    /**
     * Adding a realm to the realmlist
     * @param realm The realm
     */
    public static void addRealm(Realm realm){
        realms.add(realm);
    }

    /**
     * Creates a new realm for the given version if it doesn't exist already
     * 
     * @param version The WoW client version, such as 335
     */
    public static void addVersionRealm(ClientVersion version){
        for(Realm realm : realms)
            if(realm.getVersion() == version)
                return;
        realms.add(new Realm(realms.size()+1, "Nostalrius Memories", Server.realmlist, 8100 + realms.size(), version));
    }

    /**
     * @return The realm that belongs to the given ID
     */
    public static Realm getRealm(int id){
        return realms.get(id);
    }

    /**
     * @return All clients from all realms
     */
    public static ArrayList<Client> getAllClientsAllRealms(){
        ArrayList<Client> allClients = new ArrayList<Client>();
        for(Realm realm : realms)
            allClients.addAll(realm.getAllClients());
        return allClients;
    }
}
