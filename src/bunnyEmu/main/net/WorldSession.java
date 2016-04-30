package bunnyEmu.main.net;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import bunnyEmu.main.Server;
import bunnyEmu.main.entities.Client;
import bunnyEmu.main.entities.Realm;
import bunnyEmu.main.entities.character.Char;
import bunnyEmu.main.entities.packet.ClientPacket;
import bunnyEmu.main.entities.packet.ServerPacket;
import bunnyEmu.main.enums.ClientVersion;
import bunnyEmu.main.net.packets.client.CMSG_CHAR_CREATE;
import bunnyEmu.main.net.packets.client.CMSG_MESSAGECHAT;
import bunnyEmu.main.net.packets.client.CMSG_MOVEMENT;
import bunnyEmu.main.net.packets.client.CMSG_PLAYER_LOGIN;
import bunnyEmu.main.net.packets.server.SMSG_ACCOUNT_DATA_TIMES;
import bunnyEmu.main.net.packets.server.SMSG_MESSAGECHAT;
import bunnyEmu.main.net.packets.server.SMSG_MOTD;
import bunnyEmu.main.net.packets.server.SMSG_MOVE_SET_CANFLY;
import bunnyEmu.main.net.packets.server.SMSG_MOVE_UPDATE;
import bunnyEmu.main.net.packets.server.SMSG_NAME_CACHE;
import bunnyEmu.main.net.packets.server.SMSG_NAME_QUERY_RESPONSE;
import bunnyEmu.main.net.packets.server.SMSG_NEW_WORLD;
import bunnyEmu.main.net.packets.server.SMSG_PONG;
import bunnyEmu.main.net.packets.server.SMSG_REALM_CACHE;
import bunnyEmu.main.net.packets.server.SMSG_UPDATE_OBJECT_CREATE;
import bunnyEmu.main.utils.AuthCodes;
import bunnyEmu.main.utils.BitPack;
import bunnyEmu.main.utils.BitUnpack;
import bunnyEmu.main.utils.Opcodes;
import misc.Logger;

/**
 * Used after world authentication, handles incoming packets.
 * TODO: Handle packets in different classes (when things are getting on larger scale)
 *
 * @author Marijn
 *
 */

public class WorldSession extends Thread {
    private WorldConnection connection;
    private Realm realm;

    private final String randomNameLexicon = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private final java.util.Random rand = new java.util.Random();

    public WorldSession(WorldConnection c, Realm realm) {
        connection = c;
        this.realm = realm;
        Server.worldSessions.add(this);
    }

    public void SendSerializedPacket(String serialized) {
        String[] parts = serialized.split("\\|");
        String[] header= parts[0].split(":");
        String[] body  = parts[1].split(" ");
        if (header.length != 2) {
            Logger.writeLog("Wrong packet header: \"" + parts[0] + "\"", Logger.LOG_TYPE_ERROR);
        }
        ServerPacket pkt = new ServerPacket("[SERIALIZED]", Integer.parseInt(header[1]));
        pkt.nOpcode = (short) Integer.parseInt(header[0]);
        for (String b: body) {
            int value = Integer.parseInt(b);
            if (value > 0xFF)
                break;
            if ((value & 0x80) != 0) // Negative - Java does not support unsigned types...
                value = -0x80 + (value & 0x7F);

            pkt.put((byte)value);
        }
        connection.sendRaw(pkt);
    }

    /**
     * Send the character list to the client.
     */
    public void sendCharacters() {
        Logger.writeLog("Sending chars", Logger.LOG_TYPE_VERBOSE);
        SendSerializedPacket("59:168|1 175 145 4 0 0 0 0 0 82 101 109 101 109 98 101 114 0 1 5 0 3 9 6 2 0 1 12 0 0 0 0 0 0 0 205 215 11 198 53 126 4 195 249 15 167 66 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 216 38 0 0 4 135 49 0 0 20 0 0 0 0 0 217 38 0 0 7 218 38 0 0 8 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 74 20 0 0 21 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 256");
    }

    /**
     * Creates a character for the client and sets attributes.
     * @throws UnsupportedEncodingException
     *
     *
     */
    public void createCharacter(CMSG_CHAR_CREATE p) throws UnsupportedEncodingException {

        ServerPacket isCharOkay = new ServerPacket(Opcodes.SMSG_CHAR_CREATE, 1);

        isCharOkay.put((byte) 0x31);    // name is okay to be used \\ 0x32 means not okay
        connection.send(isCharOkay);

        /* this needs to be sent immediately after previous packet otherwise client fails */
        connection.send(new ServerPacket(Opcodes.SMSG_CHAR_CREATE, 1, AuthCodes.CHAR_CREATE_SUCCESS));


        /* TODO: need database query to insert and for start position and map here */

        float x = 1.0f;
        float y = 1.0f;
        float z = 50.0f;
        float o = 1.0f;

        int mapID = 1;

        /* this value will come from a configuration file */
        int cStartLevel = 1;

        connection.getClient().addCharacter(new Char(p.cName, x, y, z, o, mapID, p.cHairStyle,
                p.cFaceStyle, p.cFacialHair, p.cHairColor,
                                                        p.cSkinColor, p.cRace, p.cClass, p.cGender,
                                                        cStartLevel));

        Logger.writeLog("Created new char with name: " + p.cName, Logger.LOG_TYPE_VERBOSE);
    }

    /* delete the specified character */
    public void deleteCharacter(ClientPacket p) {

        /* determine guid here from packet then respond okay */
        boolean[] guidMask = new boolean[8];
        byte[] guidBytes = new byte[8];

        BitUnpack bitUnpack = new BitUnpack(p);

        guidMask[2] = bitUnpack.getBit();
        guidMask[1] = bitUnpack.getBit();
        guidMask[5] = bitUnpack.getBit();
        guidMask[7] = bitUnpack.getBit();
        guidMask[6] = bitUnpack.getBit();

        bitUnpack.getBit();

        guidMask[3] = bitUnpack.getBit();
        guidMask[0] = bitUnpack.getBit();
        guidMask[4] = bitUnpack.getBit();

        if (guidMask[1])
            guidBytes[1] = (byte) (p.get() ^ 1);

        if (guidMask[3])
            guidBytes[3] = (byte) (p.get() ^ 1);

        if (guidMask[4])
            guidBytes[4] = (byte) (p.get() ^ 1);

        if (guidMask[0])
            guidBytes[0] = (byte) (p.get() ^ 1);

        if (guidMask[7])
            guidBytes[7] = (byte) (p.get() ^ 1);

        if (guidMask[2])
            guidBytes[2] = (byte) (p.get() ^ 1);

        if (guidMask[5])
            guidBytes[5] = (byte) (p.get() ^ 1);

        if (guidMask[6])
            guidBytes[6] = (byte) (p.get() ^ 1);


        ByteBuffer buffer = ByteBuffer.wrap(guidBytes);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        int guid = buffer.getInt();

        boolean charDeletion = connection.getClient().removeCharacter(guid);

        if (charDeletion) {
            Logger.writeLog("Deleted character with GUID = " + guid, Logger.LOG_TYPE_VERBOSE);
        }
        else {
            Logger.writeLog("Failed to delete character with GUID = " + guid, Logger.LOG_TYPE_WARNING);
        }

        ServerPacket charDeleteOkay = new ServerPacket(Opcodes.SMSG_CHAR_DELETE, 1);
        charDeleteOkay.put((byte) 0x47);    // success

        connection.send(charDeleteOkay);
    }

    /**
     * Sends initial packets after world login has been confirmed.
     */
    public void verifyLogin(CMSG_PLAYER_LOGIN p) {

        final Char character = connection.getClient().setCurrentCharacter(p.getGuid());

        if (character == null) {
            Logger.writeLog("\nPROBLEM: Character is null at login to world..\n", Logger.LOG_TYPE_WARNING);
            return;
        }

        String[] login_packets =
            {
                "311:5|0 228 20 0 0 256",
                "295:5|2 16 0 0 0 256",
                "587:26|198 0 0 0 175 145 4 0 0 0 0 0 0 1 0 0 0 175 145 4 0 0 0 0 0 0 256",
                "295:5|2 16 64 0 0 256",
                "587:26|78 9 0 0 175 145 4 0 0 0 0 0 0 1 0 0 0 175 145 4 0 0 0 0 0 0 256",
                "295:5|2 16 64 8 0 256",
                "587:26|145 19 0 0 175 145 4 0 0 0 0 0 0 1 0 0 0 175 145 4 0 0 0 0 0 0 256",
                "295:5|4 2 0 0 0 256",
                "587:26|118 35 0 0 175 145 4 0 0 0 0 0 0 1 0 0 0 175 145 4 0 0 0 0 0 0 256",
                "295:5|4 3 0 0 0 256",
                "587:26|165 35 0 0 175 145 4 0 0 0 0 0 0 1 0 0 0 175 145 4 0 0 0 0 0 0 256",
                "587:26|117 80 0 0 175 145 4 0 0 0 0 0 0 1 0 0 0 175 145 4 0 0 0 0 0 0 256",
                "587:26|118 80 0 0 175 145 4 0 0 0 0 0 0 1 0 0 0 175 145 4 0 0 0 0 0 0 256",
                "587:26|119 80 0 0 175 145 4 0 0 0 0 0 0 1 0 0 0 175 145 4 0 0 0 0 0 0 256",
                "587:26|128 81 0 0 175 145 4 0 0 0 0 0 0 1 0 0 0 175 145 4 0 0 0 0 0 0 256",
                "334:0|256",
                "311:5|0 152 58 0 0 256",
                "334:0|256",
                "566:20|0 0 0 0 82 190 11 198 188 52 255 194 7 255 166 66 67 255 188 62 256",
                "566:20|0 0 0 0 205 215 11 198 53 126 4 195 249 15 167 66 0 0 0 0 256",
                "521:128|0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 256"
                ,"542:4|0 0 0 0 256"
                ,"341:20|205 215 11 198 53 126 4 195 249 15 167 66 0 0 0 0 12 0 0 0 256"
                ,"253:32|255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 255 256"
                ,"298:145|0 35 0 155 19 0 0 198 0 0 0 73 2 0 0 118 35 0 0 203 0 0 0 204 0 0 0 128 81 0 0 187 28 0 0 11 86 0 0 147 84 0 0 26 89 0 0 148 84 0 0 203 25 0 0 165 35 0 0 89 24 0 0 78 9 0 0 102 24 0 0 103 24 0 0 81 0 0 0 37 13 0 0 194 32 0 0 156 2 0 0 77 25 0 0 78 25 0 0 2 8 0 0 98 28 0 0 99 28 0 0 10 2 0 0 117 80 0 0 118 80 0 0 119 80 0 0 120 80 0 0 234 11 0 0 175 9 0 0 145 19 0 0 0 0 256"
                ,"290:324|64 0 0 0 2 0 0 0 0 0 0 0 0 0 2 0 0 0 0 2 0 0 0 0 16 0 0 0 0 0 0 0 0 0 2 0 0 0 0 0 0 0 0 0 16 0 0 0 0 0 0 0 0 0 8 0 0 0 0 9 0 0 0 0 14 0 0 0 0 0 0 0 0 0 6 0 0 0 0 6 0 0 0 0 6 0 0 0 0 6 0 0 0 0 17 0 0 0 0 17 0 0 0 0 17 0 0 0 0 17 0 0 0 0 4 0 0 0 0 4 0 0 0 0 4 0 0 0 0 4 0 0 0 0 4 0 0 0 0 0 0 0 0 0 0 0 0 0 0 4 0 0 0 0 4 0 0 0 0 4 0 0 0 0 4 0 0 0 0 4 0 0 0 0 4 0 0 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 2 0 0 0 0 20 0 0 0 0 16 0 0 0 0 2 0 0 0 0 0 0 0 0 0 16 0 0 0 0 16 0 0 0 0 16 0 0 0 0 6 0 0 0 0 24 0 0 0 0 14 0 0 0 0 0 0 0 0 0 16 0 0 0 0 16 0 0 0 0 2 0 0 0 0 16 0 0 0 0 2 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 256"
                ,"66:8|213 177 53 16 138 136 136 60 256"
                ,"297:480|203 25 0 0 73 2 0 0 2 8 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 159 0 0 128 22 8 0 " +
                    "128 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 " +
                    "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 " +
                    "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 " +
                    "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 " +
                    "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 " +
                    "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 " +
                    "0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 0 256"
                ,"502:453|49 4 0 0 120 218 117 83 61 75 3 65 16 157 189 187 196 68 139 164 136 145 160 98 192 32 10 17 226 71 68 36 152 213 206 223 96 17 16 11 11 35 4 196 70 144 116 87 216 68 68 44 20 34 54 54 130 101 218 67 65 108 132 32 130 133 133 41 36 4 9 33 132 52 130 160 51 183 123 178 228 46 3 239 118 103 118 222 237 204 238 219 32 160 105 230 215 97 149 179 48 163 121 110 13 108 195 16 13 92 199 79 218 142 20 179 119 39 6 16 236 68 179 222 67 178 0 234 10 233 101 196 77 154 148 208 204 134 155 220 80 200 139 30 59 198 36 52 179 233 46 183 169 144 23 60 203 109 185 119 108 41 164 132 7 41 34 161 153 109 149 204 236 212 182 66 46 247 144 125 232 253 162 105 102 199 77 236 40 196 104 160 31 177 171 18 227 54 177 171 150 59 234 46 151 160 15 160 103 20 104 113 254 145 193 243 219 208 99 250 200 120 248 14 221 172 195 191 197 57 64 13 81 226 133 177 115 78 126 45 180 193 137 62 51 12 214 254 184 37 218 193 93 173 15 227 62 148 137 1 247 131 98 44 12 16 6 166 70 196 145 100 32 5 109 29 188 172 86 201 123 47 244 177 226 142 161 122 89 250 166 132 67 253 198 100 152 238 250 10 177 45 239 157 230 13 93 248 142 226 24 158 42 158 115 224 7 127 18 148 241 221 1 128 170 68 224 246 114 21 91 205 206 97 156 176 247 30 229 149 252 49 135 150 216 35 162 72 33 42 69 56 75 121 136 9 74 153 6 24 4 129 74 126 20 185 155 156 238 85 15 250 53 186 191 180 124 13 139 178 198 132 242 188 156 23 227 136 223 209 177 35 77 71 101 142 104 28 13 148 176 137 37 187 23 146 140 15 150 237 57 131 156 140 109 225 152 100 73 118 45 253 79 57 150 153 200 163 194 94 67 103 124 229 224 130 79 61 157 114 82 133 56 230 62 224 66 152 127 118 232 178 78 256"
            };
        for (String l: login_packets)
            SendSerializedPacket(l);

        sendMOTD("Welcome to your Nostalrius Memories.");
        sendMOTD("Support official legacy realms here:");
        sendMOTD("https://www.change.org/p/mike-morhaime-legacy-server-among-world-of-warcraft-community");
    }

    /**
     * Synch the server and client times.
     */
    public void sendAccountDataTimes(int mask) {
        connection.send(new SMSG_ACCOUNT_DATA_TIMES(mask));
        if (this.realm.getVersion() != ClientVersion.VERSION_MOP)
            connection.send(new ServerPacket(Opcodes.SMSG_TIME_SYNC_REQ, 4));
    }

    /**
     * Send a message to the player in Message Of The Day style
     */
    public void sendMOTD(String message) {
        connection.send(new SMSG_MOTD(message));
    }

    /**
     * Response to Ping
     */
    public void sendPong() {
        connection.send(new SMSG_PONG());
    }

    /**
     * Response the name request
     */
    public void sendNameResponse() {
        connection.send(new SMSG_NAME_QUERY_RESPONSE(connection.client.getCurrentCharacter()));
    }

    /**
     * Character data? Required for MoP
     */
    public void handleNameCache(ClientPacket p){
        //long guid = p.getLong();
        //Log.log("GUID: " + guid);

        connection.send(new SMSG_NAME_CACHE(connection.client.getCurrentCharacter()));
    }

    /**
     * Realm data? Required for MoP
     */
    public void handleRealmCache(ClientPacket p) {
        int realmId = p.getInt();
        if(realm.id != realmId)
            return;

        connection.send(new SMSG_REALM_CACHE(realm));
    }

    /**
     * Handles a chat message given by the client, checks for commands.
     */
    public void handleChatMessage(CMSG_MESSAGECHAT p) {
        Char character = connection.client.getCurrentCharacter();
        Logger.writeLog("msg: " + p.getMessage(), Logger.LOG_TYPE_VERBOSE);
        connection.send(new SMSG_MESSAGECHAT(connection.client.getCurrentCharacter(), p.getLanguage(), p.getMessage()));

         try {
             if (p.getMessage().contains(".tele")) {
                 String[] coords = p.getMessage().split("\\s");
                 int mapId = Integer.parseInt(coords[1]);
                 float x = Float.parseFloat(coords[2]);
                 float y = Float.parseFloat(coords[3]);
                 float z = Float.parseFloat(coords[4]);
                 teleportTo(-x, -y, z, mapId);
             } else if (p.getMessage().contains(".speed")) {
                 String[] coords = p.getMessage().split("\\s");
                 int speed = Integer.parseInt(coords[1]);
                 character.setCharSpeed((speed > 0) ? speed : 0);
                 this.sendMOTD("Modifying the multiplying speed requires a teleport to be applied.");
             }else if (p.getMessage().contains(".fly")) {
                 connection.send(new SMSG_MOVE_SET_CANFLY(character));
             }
         } catch (Exception e) {
             this.sendMOTD("Invalid command!");
         }
    }

    /**
     * Instantly teleports the client to the given coords
     */
    public void teleportTo(float x, float y, float z, int mapId) {
        Char character = this.connection.getClient().getCurrentCharacter();
        character.setPosition(x, y, z, mapId);
        connection.send(new SMSG_NEW_WORLD(character));
        connection.send(new SMSG_UPDATE_OBJECT_CREATE(this.connection.getClient(), true));
        connection.send(new SMSG_MOVE_SET_CANFLY(character));
        multiplayerCreation();
    }

    private void multiplayerCreation(){
        realm.sendAllClients(new SMSG_UPDATE_OBJECT_CREATE(connection.getClient(), false), connection.getClient());
        for(Client client : realm.getAllClients())
            if(!client.equals(connection.getClient()) && client.isInWorld())
                connection.send(new SMSG_UPDATE_OBJECT_CREATE(client, false));
    }

    public void handleMovement(CMSG_MOVEMENT p){
        connection.getClient().getCurrentCharacter().getPosition().set(p.getPosition());
        realm.sendAllClients(new SMSG_MOVE_UPDATE(connection.getClient().getCurrentCharacter(), p.getMovementValues(), p.getPosition()), connection.getClient());
    }

    /**
     *
     * Untested and unimplemented packets go down here
     *
     */

    /* temporary proof of concept */
    private String randomName() {
        StringBuilder builder = new StringBuilder();
        while (builder.toString().length() == 0) {
            int length = rand.nextInt(5) + 5;
            for(int i = 0; i < length; i++) {
                builder.append(randomNameLexicon.charAt(rand.nextInt(randomNameLexicon.length())));
            }
        }
        return builder.toString();
    }

    public void sendRandomName() {
        /* this is temp and should actually come from the client db */
        String generatedName = randomName();

        // char name must min size 3 and max size 12
        while ((generatedName.length() < 3) && (generatedName.length() > 12)) {
            generatedName = randomName();
        }

        ServerPacket randomCharName = new ServerPacket(Opcodes.SMSG_RANDOM_NAME_RESULT, 14);
        BitPack bitPack = new BitPack(randomCharName);

        bitPack.write(generatedName.length(), 6);
        bitPack.write(1);

        bitPack.flush();

        randomCharName.putString(generatedName);

        connection.send(randomCharName);
    }
}
