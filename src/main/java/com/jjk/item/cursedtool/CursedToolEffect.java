package com.jjk.item.cursedtool;

public record CursedToolEffect(
        float damageMultiplier,
        float ceRegenBonus,
        boolean nullifyTechnique,
        boolean isSoulDirect,
        boolean bypassRCT,
        int resonanceCharges) {

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private float damageMultiplier  = 1.0f;
        private float ceRegenBonus      = 0.0f;
        private boolean nullifyTechnique = false;
        private boolean isSoulDirect     = false;
        private boolean bypassRCT        = false;
        private int resonanceCharges     = 0;

        private Builder() {}

        public Builder damageMultiplier(float v)  { this.damageMultiplier  = v;    return this; }
        public Builder ceRegenBonus(float v)       { this.ceRegenBonus      = v;    return this; }
        public Builder nullifyTechnique()           { this.nullifyTechnique  = true; return this; }
        public Builder soulDirect()                 { this.isSoulDirect      = true; return this; }
        public Builder bypassRCT()                  { this.bypassRCT         = true; return this; }
        public Builder resonanceCharges(int v)      { this.resonanceCharges  = v;    return this; }

        public CursedToolEffect build() {
            return new CursedToolEffect(
                    damageMultiplier, ceRegenBonus,
                    nullifyTechnique, isSoulDirect, bypassRCT, resonanceCharges);
        }
    }
}
