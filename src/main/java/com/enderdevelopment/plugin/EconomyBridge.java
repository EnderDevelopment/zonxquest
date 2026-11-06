package com.enderdevelopment.plugin;

import org.bukkit.OfflinePlayer;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.lang.reflect.Method;

public final class EconomyBridge {
    private final Main plugin;
    private Object economy;
    private Method deposit;

    public EconomyBridge(Main plugin) { this.plugin = plugin; refresh(); }

    public void refresh() {
        for (RegisteredServiceProvider<?> provider : plugin.getServer().getServicesManager().getRegistrations(Object.class)) {
            Object service = provider.getProvider();
            if (service.getClass().getName().equals("net.milkbowl.vault.economy.Economy") || hasDepositMethod(service)) {
                economy = service;
                try { deposit = service.getClass().getMethod("depositPlayer", OfflinePlayer.class, double.class); } catch (ReflectiveOperationException ignored) { economy = null; }
                return;
            }
        }
    }

    public boolean deposit(OfflinePlayer player, double amount) {
        if (economy == null || deposit == null) return false;
        try { deposit.invoke(economy, player, amount); return true; } catch (ReflectiveOperationException exception) { return false; }
    }

    private boolean hasDepositMethod(Object service) {
        for (Method method : service.getClass().getMethods()) if (method.getName().equals("depositPlayer") && method.getParameterCount() == 2) return true;
        return false;
    }
}
