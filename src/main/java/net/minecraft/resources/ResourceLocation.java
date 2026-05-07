package net.minecraft.resources;

import java.util.Objects;

/**
 * Compatibility shim for environments where Mojang's ResourceLocation is named {@link Identifier}.
 * <p>
 * This is only intended to satisfy optional third-party APIs (e.g., JEI) that still reference
 * {@code net.minecraft.resources.ResourceLocation}.
 */
public final class ResourceLocation {
    private final Identifier id;

    private ResourceLocation(Identifier id) {
        this.id = id;
    }

    public static ResourceLocation fromNamespaceAndPath(String namespace, String path) {
        return new ResourceLocation(Identifier.fromNamespaceAndPath(namespace, path));
    }

    public static ResourceLocation parse(String text) {
        return new ResourceLocation(Identifier.parse(text));
    }

    public String getNamespace() {
        return id.getNamespace();
    }

    public String getPath() {
        return id.getPath();
    }

    public Identifier toIdentifier() {
        return id;
    }

    @Override
    public String toString() {
        return id.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResourceLocation that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}

