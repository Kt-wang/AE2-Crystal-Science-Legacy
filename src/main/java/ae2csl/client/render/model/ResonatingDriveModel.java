/*
 * AE2 Crystal Science - ResonatingDriveModel (1.12.2)
 * 复制自 AE2-Supergiant 的 DriveModel（LGPL），差异：
 *  - 外壳模型改为 ae2csl:block/resonating_drive（drive_base 复刻 + 谐振青蓝染色贴图）
 *  - 烘焙产物为 ResonatingDriveBakedModel
 * 舱内元件底盘模型仍走公共 API StorageCellModels（AE2 元件自动包含，
 * 第三方元件可通过 StorageCellModels.registerModel 注册）。
 */
package ae2csl.client.render.model;

import ae2.api.client.StorageCellModels;
import ae2.client.render.BasicUnbakedModel;
import ae2.core.AppEng;
import ae2csl.Tags;
import com.google.common.collect.ImmutableSet;
import it.unimi.dsi.fastutil.objects.Reference2ObjectOpenHashMap;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.vertex.VertexFormat;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.ModelLoaderRegistry;
import net.minecraftforge.common.model.IModelState;
import net.minecraftforge.common.model.TRSRTransformation;

import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@SuppressWarnings("deprecation")
public class ResonatingDriveModel implements BasicUnbakedModel {
    private static final ResourceLocation MODEL_BASE =
        new ResourceLocation(Tags.MOD_ID, "block/resonating_drive");
    private static final ResourceLocation MODEL_CELL_EMPTY = AppEng.makeId("block/drive/drive_cell_empty");

    @Override
    public Collection<ResourceLocation> getDependencies() {
        return ImmutableSet.<ResourceLocation>builder()
                           .add(MODEL_BASE)
                           .add(MODEL_CELL_EMPTY)
                           .add(StorageCellModels.getDefaultModel())
                           .addAll(StorageCellModels.models().values())
                           .build();
    }

    @Override
    public IBakedModel bake(@NotNull IModelState state, @NotNull VertexFormat format,
                            @NotNull Function<ResourceLocation, TextureAtlasSprite> bakedTextureGetter) {
        Map<Item, IBakedModel> cellModels = new Reference2ObjectOpenHashMap<>();

        try {
            for (var entry : StorageCellModels.models().entrySet()) {
                cellModels.put(entry.getKey(),
                    ModelLoaderRegistry.getModel(entry.getValue()).bake(state, format, bakedTextureGetter));
            }

            IBakedModel baseModel = ModelLoaderRegistry.getModel(MODEL_BASE).bake(state, format, bakedTextureGetter);
            IBakedModel defaultCellModel = ModelLoaderRegistry.getModel(StorageCellModels.getDefaultModel())
                                                              .bake(state, format, bakedTextureGetter);
            cellModels.put(Items.AIR,
                ModelLoaderRegistry.getModel(MODEL_CELL_EMPTY).bake(state, format, bakedTextureGetter));
            TRSRTransformation transform = state.apply(Optional.empty())
                                                .orElse(TRSRTransformation.identity());
            return new ResonatingDriveBakedModel(baseModel, cellModels, defaultCellModel, transform);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
