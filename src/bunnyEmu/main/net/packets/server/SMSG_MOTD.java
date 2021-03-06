package bunnyEmu.main.net.packets.server;

import bunnyEmu.main.entities.packet.ServerPacket;
import bunnyEmu.main.utils.BitPack;
import bunnyEmu.main.utils.Opcodes;

/**
 * Message of the day message
 * 
 * @author Marijn
 *
 */
public class SMSG_MOTD extends ServerPacket {
	
	String message;
	
	public SMSG_MOTD(String message) {
		super(Opcodes.SMSG_MOTD, 20 +  message.length() + 1);
		this.message = message;
	}

	public boolean writeVanilla(){
		sOpcode = Opcodes.SMSG_MESSAGECHAT;
		this.size = (short) (this.message.length() + 1 + 18);

		byte CHAT_MSG_SYSTEM = 0x0A;
		int LANG_UNIVERSAL = 0;
		long targetGuid = 0;
		put(CHAT_MSG_SYSTEM);
		putInt(LANG_UNIVERSAL);
		putLong(targetGuid);
		putInt(this.message.length() + 1);
		putString(this.message);
		put((byte)0);
		wrap();
		return true;
	}

	public boolean writeGeneric(){
		putInt(4);
		putString(message);
		return true;
	}
	
	public boolean writeMoP(){
		BitPack bitPack = new BitPack(this);

		bitPack.write(3, 4);
		bitPack.write(message.length(), 7);
		bitPack.flush();
		putString(message);

		wrap();
		return true;
	}

}
