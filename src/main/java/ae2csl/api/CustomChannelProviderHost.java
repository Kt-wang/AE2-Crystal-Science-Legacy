/*
 * AE2 Crystal Science - API CustomChannelProviderHost Interface
 * Ported from 1.21.1 source
 *
 * Hosts that can provide custom max channel capacity.
 */
package ae2csl.api;

public interface CustomChannelProviderHost {

    /**
     * Get the max channels this host can carry.
     */
    int getMaxChannels();

    /**
     * Set the max channels.
     */
    void setMaxChannels(int maxChannels);

    /**
     * Whether custom channel capacity is enabled.
     */
    boolean isEnabledCustomChannel();

    /**
     * Set the custom channel enabled state.
     */
    void setEnabledCustomChannel(boolean enabled);
}
