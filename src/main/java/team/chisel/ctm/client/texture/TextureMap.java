package team.chisel.ctm.client.texture;

import com.google.common.base.Preconditions;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import team.chisel.ctm.api.client.Renderable;
import team.chisel.ctm.api.client.TextureContext;
import team.chisel.ctm.api.client.TextureInfo;
import team.chisel.ctm.client.render.RenderableArray;
import team.chisel.ctm.client.render.SpriteUnbakedQuad;
import team.chisel.ctm.client.render.Submap;
import team.chisel.ctm.client.render.SubmapImpl;
import team.chisel.ctm.client.texture.context.TextureContextGrid;
import team.chisel.ctm.client.texture.context.TextureContextGrid.Point2i;
import team.chisel.ctm.client.texture.type.TextureTypeMap;

public class TextureMap extends AbstractTexture<TextureTypeMap> {
	private final int xSize;
	private final int ySize;
	private final int xOffset;
	private final int yOffset;
	private final MapType mapType;

	public TextureMap(TextureTypeMap type, TextureInfo info) {
		super(type, info);
		mapType = type.getType();
		if (info.getInfo().isPresent()) {
			JsonObject jsonObject = info.getInfo().get();
			if (jsonObject.has("width") && jsonObject.has("height")) {
				Preconditions.checkArgument(jsonObject.get("width").isJsonPrimitive() && jsonObject.get("width").getAsJsonPrimitive().isNumber(), "width must be a number!");
				Preconditions.checkArgument(jsonObject.get("height").isJsonPrimitive() && jsonObject.get("height").getAsJsonPrimitive().isNumber(), "height must be a number!");
				xSize = jsonObject.get("width").getAsInt();
				ySize = jsonObject.get("height").getAsInt();
			} else if (jsonObject.has("size")) {
				Preconditions.checkArgument(jsonObject.get("size").isJsonPrimitive() && jsonObject.get("size").getAsJsonPrimitive().isNumber(), "size must be a number!");
				xSize = jsonObject.get("size").getAsInt();
				ySize = jsonObject.get("size").getAsInt();
			} else {
				xSize = ySize = 2;
			}
			if (jsonObject.has("x_offset")) {
				Preconditions.checkArgument(jsonObject.get("x_offset").isJsonPrimitive() && jsonObject.get("x_offset").getAsJsonPrimitive().isNumber(), "x_offset must be a number!");
				xOffset = jsonObject.get("x_offset").getAsInt();
			} else {
				xOffset = 0;
			}
			if (jsonObject.has("y_offset")) {
				Preconditions.checkArgument(jsonObject.get("y_offset").isJsonPrimitive() && jsonObject.get("y_offset").getAsJsonPrimitive().isNumber(), "y_offset must be a number!");
				yOffset = jsonObject.get("y_offset").getAsInt();
			} else {
				yOffset = 0;
			}
		} else {
			xOffset = yOffset = 0;
			xSize = ySize = 2;
		}
		Preconditions.checkArgument(xSize > 0 && ySize > 0, "Cannot have a dimension of 0!");
	}

	@Override
	public Renderable transformQuad(BakedQuad bakedQuad, TextureContext context, int quadGoal, Direction cullFace) {
		return mapType.transformQuad(this, bakedQuad, context, quadGoal, cullFace);
	}

	public int getXSize() {
		return xSize;
	}

	public int getYSize() {
		return ySize;
	}

	public int getXOffset() {
		return xOffset;
	}

	public int getYOffset() {
		return yOffset;
	}

	public interface MapType {
		Renderable transformQuad(TextureMap texture, BakedQuad bakedQuad, @Nullable TextureContext context, int quadGoal, Direction cullFace);

		@NotNull
		TextureContext getContext(@NotNull BlockPos pos, @NotNull TextureMap texture);
	}

	// TODO the enum fields are very similar
	public enum MapTypeImpl implements MapType {
		RANDOM {
			@Override
			public Renderable transformQuad(TextureMap texture, BakedQuad bakedQuad, @Nullable TextureContext context, int quadGoal, Direction cullFace) {
				Point2i textureCoords;
				if (context instanceof TextureContextGrid) {
					textureCoords = ((TextureContextGrid) context).getTextureCoords(bakedQuad.getFace());
				} else {
					textureCoords = new Point2i(1, 1);
				}

				float intervalX = 16.0F / texture.getXSize();
				float intervalY = 16.0F / texture.getYSize();
				float maxX = intervalX * textureCoords.getX();
				float maxY = intervalY * textureCoords.getY();
				Submap submap = new SubmapImpl(intervalX, intervalY, maxX - intervalX, maxY - intervalY);

				SpriteUnbakedQuad quad = texture.unbake(bakedQuad, cullFace);
				if (quadGoal == 4) {
					SpriteUnbakedQuad[] quads = quad.toQuadrants();
					for (int i = 0; i < quads.length; i++) {
						if (quads[i] != null) {
							quads[i].setUVBounds(texture.sprites[0]);
							quads[i].applySubmap(submap);
						}
					}
					return new RenderableArray(quads);
				} else {
					quad.setUVBounds(texture.sprites[0]);
					quad.applySubmap(submap);
					return quad;
				}
			}

			@Override
			public TextureContext getContext(@NotNull BlockPos pos, @NotNull TextureMap texture) {
				return new TextureContextGrid.TextureContextRandom(pos, texture, true);
			}
		},
		PATTERNED {
			@Override
			public Renderable transformQuad(TextureMap texture, BakedQuad bakedQuad, @Nullable TextureContext context, int quadGoal, Direction cullFace) {
				Point2i textureCoords;
				if (context instanceof TextureContextGrid) {
					textureCoords = ((TextureContextGrid) context).getTextureCoords(bakedQuad.getFace());
				} else {
					textureCoords = new Point2i(0, 0);
				}

				float intervalX = 16.0F / texture.getXSize();
				float intervalY = 16.0F / texture.getYSize();
				float minX = intervalX * textureCoords.getX();
				float minY = intervalY * textureCoords.getY();

				Submap submap = new SubmapImpl(intervalX, intervalY, minX, minY);
				SpriteUnbakedQuad quad = texture.unbake(bakedQuad, cullFace);
				if (quadGoal == 4) {
					SpriteUnbakedQuad[] quads = quad.toQuadrants();
					for (int i = 0; i < quads.length; i++) {
						if (quads[i] != null) {
							quads[i].setUVBounds(texture.sprites[0]);
							quads[i].applySubmap(submap);
						}
					}
					return new RenderableArray(quads);
				} else {
					quad.setUVBounds(texture.sprites[0]);
					quad.applySubmap(submap);
					return quad;
				}
			}

			@Override
			public TextureContext getContext(@NotNull BlockPos pos, @NotNull TextureMap texture) {
				return new TextureContextGrid.TextureContextPatterned(pos, texture, true);
			}
		};
	}
}
