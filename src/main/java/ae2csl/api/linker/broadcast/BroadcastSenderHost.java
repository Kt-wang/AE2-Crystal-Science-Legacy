/*
 * AE2 Crystal Science - API BroadcastSenderHost Interface
 * Ported from 1.21.1 source
 *
 * Objects implementing this can act as broadcast senders.
 */
package ae2csl.api.linker.broadcast;

public interface BroadcastSenderHost extends BandLinkHost {

    /**
     * How many channels this sender can provide to the band.
     */
    int getCouldSendChannels();
}
