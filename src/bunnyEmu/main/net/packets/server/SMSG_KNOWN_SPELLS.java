package bunnyEmu.main.net.packets.server;

import java.util.ArrayList;

import bunnyEmu.main.entities.character.Char;
import bunnyEmu.main.entities.character.Spell;
import bunnyEmu.main.entities.packet.ServerPacket;
import bunnyEmu.main.utils.BitPack;
import bunnyEmu.main.utils.Opcodes;

/**
 * Sends all the spells known by the character
 * 
 * @author Marijn
 *
 */
public class SMSG_KNOWN_SPELLS extends ServerPacket {

	public SMSG_KNOWN_SPELLS(Char character) {
		super(Opcodes.SMSG_KNOWN_SPELLS, 50);
	}
}
