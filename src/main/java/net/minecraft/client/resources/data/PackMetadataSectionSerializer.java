package net.minecraft.client.resources.data;

import com.google.gson.*;
import net.minecraft.util.IChatComponent;
import net.minecraft.util.JsonUtils;

import java.lang.reflect.Type;

public class PackMetadataSectionSerializer
extends BaseMetadataSectionSerializer<PackMetadataSection>
implements JsonSerializer<PackMetadataSection> {
    public PackMetadataSection deserialize(JsonElement p_deserialize_1_, Type p_deserialize_2_, JsonDeserializationContext p_deserialize_3_) throws JsonParseException {
        JsonObject jsonobject = p_deserialize_1_.getAsJsonObject();
        IChatComponent ichatcomponent = (IChatComponent)p_deserialize_3_.deserialize(jsonobject.get("description"), IChatComponent.class);
        if (ichatcomponent == null) {
            throw new JsonParseException("Invalid/missing description!");
        }
        int i = JsonUtils.getInt(jsonobject, "pack_format");
        return new PackMetadataSection(ichatcomponent, i);
    }

    public JsonElement serialize(PackMetadataSection packMeta, Type type, JsonSerializationContext context) {
        JsonObject jsonobject = new JsonObject();
        jsonobject.addProperty("pack_format", (Number)packMeta.getPackFormat());
        jsonobject.add("description", context.serialize((Object)packMeta.getPackDescription()));
        return jsonobject;
    }

    @Override
    public String getSectionName() {
        return "pack";
    }
}

