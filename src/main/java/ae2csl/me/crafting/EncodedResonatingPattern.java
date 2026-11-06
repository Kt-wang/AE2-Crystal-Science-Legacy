/*
 * AE2 Crystal Science - EncodedResonatingPattern (1.12.2)
 * Ported from 1.21.1 source (record → class, Codec → NBT)
 *
 * Stores encoded resonating pattern data: sparse inputs, outputs, and per-input targets.
 */
package ae2csl.me.crafting;

import ae2.api.stacks.GenericStack;
import ae2csl.api.util.GlobalPos;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.EnumFacing;

import java.util.*;

public final class EncodedResonatingPattern {

    private final List<GenericStack> sparseInputs;
    private final List<GenericStack> sparseOutputs;
    private final List<Optional<Target>> inputTargets;

    public EncodedResonatingPattern(List<GenericStack> sparseInputs,
                                    List<GenericStack> sparseOutputs,
                                    List<Optional<Target>> inputTargets) {
        this.sparseInputs = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(sparseInputs)));
        this.sparseOutputs = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(sparseOutputs)));
        this.inputTargets = Collections.unmodifiableList(new ArrayList<>(Objects.requireNonNull(inputTargets)));

        if (inputTargets.size() != sparseInputs.size()) {
            throw new IllegalArgumentException("inputTargets must have same size as sparseInputs");
        }
    }

    public List<GenericStack> getSparseInputs() { return sparseInputs; }
    public List<GenericStack> getSparseOutputs() { return sparseOutputs; }
    public List<Optional<Target>> getInputTargets() { return inputTargets; }

    public boolean containsMissingContent() {
        // In 1.12.2, "missing content" is harder to detect; assume not missing
        return false;
    }

    public Optional<Target> targetOfSparseInput(int sparseIndex) {
        if (sparseIndex < 0 || sparseIndex >= inputTargets.size()) return Optional.empty();
        return inputTargets.get(sparseIndex);
    }

    public EncodedResonatingPattern withTarget(int sparseIndex, Target newTarget) {
        if (sparseIndex < 0 || sparseIndex >= inputTargets.size()) return this;
        List<Optional<Target>> copy = new ArrayList<>(inputTargets);
        copy.set(sparseIndex, newTarget == null ? Optional.empty() : Optional.of(newTarget));
        return new EncodedResonatingPattern(sparseInputs, sparseOutputs, copy);
    }

    // ---- Target ----
    public static final class Target {
        private final GlobalPos pos;
        private final EnumFacing face;

        public Target(GlobalPos pos, EnumFacing face) {
            this.pos = pos;
            this.face = face;
        }

        public GlobalPos getPos() { return pos; }
        public EnumFacing getFace() { return face; }

        public static Target read(NBTTagCompound tag) {
            int dim = tag.getInteger("dim");
            int x = tag.getInteger("x");
            int y = tag.getInteger("y");
            int z = tag.getInteger("z");
            EnumFacing face = tag.hasKey("face") ? EnumFacing.byIndex(tag.getInteger("face")) : null;
            return new Target(new GlobalPos(dim, new net.minecraft.util.math.BlockPos(x, y, z)), face);
        }

        public NBTTagCompound write() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setInteger("dim", pos.getDimension());
            tag.setInteger("x", pos.getPos().getX());
            tag.setInteger("y", pos.getPos().getY());
            tag.setInteger("z", pos.getPos().getZ());
            if (face != null) tag.setInteger("face", face.getIndex());
            return tag;
        }

        public void write(PacketBuffer buf) {
            buf.writeInt(pos.getDimension());
            buf.writeInt(pos.getPos().getX());
            buf.writeInt(pos.getPos().getY());
            buf.writeInt(pos.getPos().getZ());
            buf.writeBoolean(face != null);
            if (face != null) buf.writeEnumValue(face);
        }

        public static Target read(PacketBuffer buf) {
            int dim = buf.readInt();
            int x = buf.readInt(), y = buf.readInt(), z = buf.readInt();
            EnumFacing face = buf.readBoolean() ? buf.readEnumValue(EnumFacing.class) : null;
            return new Target(new GlobalPos(dim, new net.minecraft.util.math.BlockPos(x, y, z)), face);
        }
    }

    // ---- NBT Serialization ----
    public NBTTagCompound writeToNBT() {
        NBTTagCompound tag = new NBTTagCompound();

        NBTTagList inputsTag = new NBTTagList();
        for (GenericStack stack : sparseInputs) {
            inputsTag.appendTag(GenericStack.writeTag(stack));
        }
        tag.setTag("inputs", inputsTag);

        NBTTagList outputsTag = new NBTTagList();
        for (GenericStack stack : sparseOutputs) {
            outputsTag.appendTag(GenericStack.writeTag(stack));
        }
        tag.setTag("outputs", outputsTag);

        NBTTagList targetsTag = new NBTTagList();
        for (Optional<Target> opt : inputTargets) {
            NBTTagCompound tt = new NBTTagCompound();
            tt.setBoolean("present", opt.isPresent());
            if (opt.isPresent()) {
                tt.setTag("target", opt.get().write());
            }
            targetsTag.appendTag(tt);
        }
        tag.setTag("targets", targetsTag);

        return tag;
    }

    @SuppressWarnings("unchecked")
    public static EncodedResonatingPattern readFromNBT(NBTTagCompound tag) {
        List<GenericStack> inputs = new ArrayList<>();
        NBTTagList inputsTag = tag.getTagList("inputs", 10);
        for (int i = 0; i < inputsTag.tagCount(); i++) {
            inputs.add(GenericStack.readTag(inputsTag.getCompoundTagAt(i)));
        }

        List<GenericStack> outputs = new ArrayList<>();
        NBTTagList outputsTag = tag.getTagList("outputs", 10);
        for (int i = 0; i < outputsTag.tagCount(); i++) {
            outputs.add(GenericStack.readTag(outputsTag.getCompoundTagAt(i)));
        }

        List<Optional<Target>> targets = new ArrayList<>();
        NBTTagList targetsTag = tag.getTagList("targets", 10);
        for (int i = 0; i < targetsTag.tagCount(); i++) {
            NBTTagCompound tt = targetsTag.getCompoundTagAt(i);
            if (tt.getBoolean("present")) {
                targets.add(Optional.of(Target.read(tt.getCompoundTag("target"))));
            } else {
                targets.add(Optional.empty());
            }
        }

        return new EncodedResonatingPattern(inputs, outputs, targets);
    }
}
