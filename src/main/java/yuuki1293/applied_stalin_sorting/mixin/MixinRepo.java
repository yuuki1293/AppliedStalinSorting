package yuuki1293.applied_stalin_sorting.mixin;

import java.util.ArrayList;
import java.util.Comparator;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import appeng.client.gui.me.common.Repo;
import appeng.menu.me.common.GridInventoryEntry;

@Mixin(value = Repo.class, remap = false)
public class MixinRepo {

    @Redirect(
        method = "updateView",
        at = @At(value = "INVOKE", target = "Ljava/util/ArrayList;sort(Ljava/util/Comparator;)V"))
    private void redirectSort(ArrayList<GridInventoryEntry> instance, Comparator<? super GridInventoryEntry> c) {
        if (instance.size() < 2) {
            return;
        }

        GridInventoryEntry previous = instance.getFirst();
        for (int i = 1; i < instance.size();) {
            GridInventoryEntry current = instance.get(i);
            if (c.compare(previous, current) <= 0) {
                previous = current;
                i++;
            } else {
                instance.remove(i);
            }
        }
    }
}
