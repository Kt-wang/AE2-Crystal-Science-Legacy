/*
 * AE2 Crystal Science - API BroadcastReceiverHost Interface
 * Ported from 1.21.1 source
 *
 * Receiver end of a broadcast device should implement this.
 */
package ae2csl.api.linker.broadcast;

public interface BroadcastReceiverHost extends BandLinkHost {

    /**
     * How many channels this receiver expects to be assigned.
     */
    int getExpectedChannels();
}
