/*
 * AE2 Crystal Science - MultiCraftingTracker
 * Ported from 1.21.1 source (copy from AE2)
 */
package ae2csl.me.logic;

import ae2.api.networking.crafting.*;
import ae2.api.networking.security.IActionSource;
import ae2.api.stacks.AEKey;
import ae2.api.storage.StorageHelper;
import ae2.helpers.NonNullArrayIterator;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;

import com.google.common.collect.ImmutableSet;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MultiCraftingTracker {

    private final int size;
    private final ICraftingRequester owner;

    private Future<ICraftingPlan>[] jobs = null;
    private ICraftingLink[] links = null;

    @SuppressWarnings("unchecked")
    public MultiCraftingTracker(ICraftingRequester o, int size) {
        this.owner = o;
        this.size = size;
    }

    public void readFromNBT(NBTTagCompound extra) {
        for (int x = 0; x < this.size; x++) {
            NBTTagCompound link = extra.getCompoundTag("links-" + x);
            if (link != null && !link.isEmpty()) {
                this.setLink(x, StorageHelper.loadCraftingLink(link, this.owner));
            }
        }
    }

    public void writeToNBT(NBTTagCompound extra) {
        for (int x = 0; x < this.size; x++) {
            ICraftingLink link = this.getLink(x);
            if (link != null) {
                NBTTagCompound ln = new NBTTagCompound();
                link.writeToNBT(ln);
                extra.setTag("links-" + x, ln);
            }
        }
    }

    public boolean handleCrafting(int x, AEKey what, long amount, World level, ICraftingService cg, IActionSource mySrc) {
        Future<ICraftingPlan> craftingJob = this.getJob(x);
        if (this.getLink(x) != null) return false;

        if (craftingJob != null) {
            try {
                ICraftingPlan job = null;
                if (craftingJob.isDone()) {
                    job = craftingJob.get();
                }
                if (job != null) {
                    ICraftingSubmitResult result = cg.submitJob(job, this.owner, null, false, mySrc);
                    this.setJob(x, null);
                    if (result.successful()) {
                        this.setLink(x, result.link());
                        return true;
                    }
                }
            } catch (InterruptedException | ExecutionException ignored) {}
        } else if (this.getLink(x) == null) {
            this.setJob(x,
                    cg.beginCraftingCalculation(level, () -> mySrc, what, amount, CalculationStrategy.CRAFT_LESS));
        }
        return false;
    }

    public ImmutableSet<ICraftingLink> getRequestedJobs() {
        if (this.links == null) return ImmutableSet.of();
        return ImmutableSet.copyOf(new NonNullArrayIterator<>(this.links));
    }

    public void jobStateChange(ICraftingLink link) {
        if (this.links != null) {
            for (int x = 0; x < this.links.length; x++) {
                if (this.links[x] == link) {
                    this.setLink(x, null);
                    return;
                }
            }
        }
    }

    public int getSlot(ICraftingLink link) {
        if (this.links != null) {
            for (int x = 0; x < this.links.length; x++) {
                if (this.links[x] == link) return x;
            }
        }
        return -1;
    }

    public void cancel() {
        if (this.links != null) {
            for (ICraftingLink l : this.links) {
                if (l != null) l.cancel();
            }
            this.links = null;
        }
        if (this.jobs != null) {
            for (Future<ICraftingPlan> l : this.jobs) {
                if (l != null) l.cancel(true);
            }
            this.jobs = null;
        }
    }

    public boolean isBusy(int slot) {
        return this.getLink(slot) != null || this.getJob(slot) != null;
    }

    private ICraftingLink getLink(int slot) {
        if (this.links == null) return null;
        return this.links[slot];
    }

    @SuppressWarnings("unchecked")
    private void setLink(int slot, ICraftingLink l) {
        if (this.links == null) this.links = new ICraftingLink[this.size];
        this.links[slot] = l;
        boolean hasStuff = false;
        for (int x = 0; x < this.links.length; x++) {
            ICraftingLink g = this.links[x];
            if (g == null || g.isCanceled() || g.isDone()) {
                this.links[x] = null;
            } else {
                hasStuff = true;
            }
        }
        if (!hasStuff) this.links = null;
    }

    private Future<ICraftingPlan> getJob(int slot) {
        if (this.jobs == null) return null;
        return this.jobs[slot];
    }

    @SuppressWarnings("unchecked")
    private void setJob(int slot, Future<ICraftingPlan> l) {
        if (this.jobs == null) this.jobs = new Future[this.size];
        this.jobs[slot] = l;
        boolean hasStuff = false;
        for (Future<ICraftingPlan> job : this.jobs) {
            if (job != null) {
                hasStuff = true;
                break;
            }
        }
        if (!hasStuff) this.jobs = null;
    }
}
