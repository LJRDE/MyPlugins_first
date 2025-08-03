package Dim_LJR;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntitySpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class myPlugins_first extends JavaPlugin implements Listener {

    // 常量定义
    private static final String WEAPON_STONE_I_NAME = ChatColor.LIGHT_PURPLE + "武器强化石Ⅰ";
    private static final String WEAPON_STONE_II_NAME = ChatColor.DARK_PURPLE + "武器强化石Ⅱ";
    private static final String ARMOR_STONE_I_NAME = ChatColor.BLUE + "护甲强化石Ⅰ";
    private static final String ARMOR_STONE_II_NAME = ChatColor.DARK_BLUE + "护甲强化石Ⅱ";
    private static final List<String> STONE_LORE = Arrays.asList(
            ChatColor.GRAY + "右键点击对应装备使用",
            ChatColor.GRAY + "提升装备属性"
    );

    @EventHandler
    public void onEntitySpawn(EntitySpawnEvent event) {
        if (event.getEntityType() == EntityType.ZOMBIE || event.getEntityType() == EntityType.SKELETON) {
            LivingEntity entity = (LivingEntity) event.getEntity();
            double roll = random.nextDouble();

            // 钻石套装怪物 (2%)
            if (roll < DIAMOND_ZOMBIE_CHANCE) {
                equipDiamondArmor(entity);
                specialMobs.add(entity.getUniqueId());
                scheduleDespawn(entity);
            }
            // 铁套装怪物 (5%)
            else if (roll < IRON_ZOMBIE_CHANCE) {
                equipIronArmor(entity);
                specialMobs.add(entity.getUniqueId());
                scheduleDespawn(entity);
            }
        }
    }

    private void scheduleDespawn(LivingEntity entity) {
        if (!config.getBoolean("despawn.enabled")) return;

        BukkitRunnable task = new BukkitRunnable() {
            @Override
            public void run() {
                if (entity.isValid()) {
                    entity.remove();
                    specialMobs.remove(entity.getUniqueId());
                }
                despawnTasks.remove(entity.getUniqueId());
            }
        };

        task.runTaskLater(this, config.getInt("despawn.time"));
        despawnTasks.put(entity.getUniqueId(), task);
    }

    private void equipIronArmor(LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(new ItemStack(Material.IRON_HELMET));
            equipment.setChestplate(new ItemStack(Material.IRON_CHESTPLATE));
            equipment.setLeggings(new ItemStack(Material.IRON_LEGGINGS));
            equipment.setBoots(new ItemStack(Material.IRON_BOOTS));

            if (entity instanceof Zombie) {
                equipment.setItemInMainHand(new ItemStack(Material.IRON_SWORD));
            } else if (entity instanceof Skeleton) {
                equipment.setItemInMainHand(new ItemStack(Material.BOW));
            }

            // 设置不掉落
            equipment.setHelmetDropChance(0);
            equipment.setChestplateDropChance(0);
            equipment.setLeggingsDropChance(0);
            equipment.setBootsDropChance(0);
            equipment.setItemInMainHandDropChance(0);
        }

        // 强化属性
        enhanceMob(entity, 2.0, 2.0, 1.1);
    }

    private void equipDiamondArmor(LivingEntity entity) {
        EntityEquipment equipment = entity.getEquipment();
        if (equipment != null) {
            equipment.setHelmet(new ItemStack(Material.DIAMOND_HELMET));
            equipment.setChestplate(new ItemStack(Material.DIAMOND_CHESTPLATE));
            equipment.setLeggings(new ItemStack(Material.DIAMOND_LEGGINGS));
            equipment.setBoots(new ItemStack(Material.DIAMOND_BOOTS));

            if (entity instanceof Zombie) {
                equipment.setItemInMainHand(new ItemStack(Material.DIAMOND_SWORD));
            } else if (entity instanceof Skeleton) {
                equipment.setItemInMainHand(new ItemStack(Material.BOW));
            }

            // 设置不掉落
            equipment.setHelmetDropChance(0);
            equipment.setChestplateDropChance(0);
            equipment.setLeggingsDropChance(0);
            equipment.setBootsDropChance(0);
            equipment.setItemInMainHandDropChance(0);
        }

        // 强化属性
        enhanceMob(entity, 3.0, 3.0, 1.2);
    }

    private void enhanceMob(LivingEntity entity, double healthMult, double damageMult, double speedMult) {
        entity.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20 * healthMult);
        entity.setHealth(20 * healthMult);
        entity.getAttribute(Attribute.GENERIC_ATTACK_DAMAGE).setBaseValue(2 * damageMult);
        entity.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0.25 * speedMult);

        // 添加发光效果
        entity.addPotionEffect(new PotionEffect(PotionEffectType.GLOWING, Integer.MAX_VALUE, 0));
    }

    @EventHandler
    public void onEntityDeath(EntityDeathEvent event) {
        if (specialMobs.contains(event.getEntity().getUniqueId())) {
            // 100% 掉落强化石
            if (event.getEntity().getEquipment() != null &&
                    event.getEntity().getEquipment().getHelmet() != null) {

                Material helmet = event.getEntity().getEquipment().getHelmet().getType();

                // 钻石套装掉落Ⅱ级强化石
                if (helmet == Material.DIAMOND_HELMET) {
                    event.getDrops().add(createUpgradeStone(2, true));
                    event.getDrops().add(createUpgradeStone(2, false));
                }
                // 铁套装掉落Ⅰ级强化石
                else if (helmet == Material.IRON_HELMET) {
                    event.getDrops().add(createUpgradeStone(1, true));
                    event.getDrops().add(createUpgradeStone(1, false));
                }
            }

            specialMobs.remove(event.getEntity().getUniqueId());
            BukkitRunnable task = despawnTasks.remove(event.getEntity().getUniqueId());
            if (task != null) task.cancel();
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        ItemStack clickedItem = event.getItem();

        if (clickedItem == null) return;

        // 检查是否是强化石
        int stoneType = getUpgradeStoneType(clickedItem);
        if (stoneType == 0) return;

        boolean isWeaponStone = clickedItem.getItemMeta().getDisplayName().contains("武器");
        ItemStack targetItem = isWeaponStone ? player.getInventory().getItemInMainHand() :
                player.getInventory().getItemInOffHand();

        if (isWeaponStone && isWeapon(targetItem.getType())) {
            upgradeWeapon(player, targetItem, stoneType);
            clickedItem.setAmount(clickedItem.getAmount() - 1);
            event.setCancelled(true);
        } else if (!isWeaponStone && isArmor(targetItem.getType())) {
            upgradeArmor(player, targetItem, stoneType);
            clickedItem.setAmount(clickedItem.getAmount() - 1);
            event.setCancelled(true);
        } else {
            player.sendMessage(ChatColor.RED + "你必须手持正确的装备才能使用强化石!");
        }
    }

    private void upgradeWeapon(Player player, ItemStack weapon, int stoneLevel) {
        ItemMeta meta = weapon.getItemMeta();

        // Ⅰ级强化石：提升锋利等级
        if (stoneLevel == 1) {
            int currentLevel = meta.getEnchantLevel(Enchantment.DAMAGE_ALL);
            int newLevel = currentLevel + 1;

            if (newLevel > MAX_ENCHANT_LEVEL) {
                player.sendMessage(ChatColor.RED + "这把武器的锋利等级已经达到最大值 " + MAX_ENCHANT_LEVEL + "!");
                return;
            }

            meta.addEnchant(Enchantment.DAMAGE_ALL, newLevel, true);
            player.sendMessage(ChatColor.GREEN + "武器强化成功! 锋利等级提升至 " + newLevel);
        }
        // Ⅱ级强化石：直接提升伤害
        else {
            double damageBonus = 1.0; // 每次提升1点伤害
            String damageKey = "weapon_damage";

            double currentDamage = meta.hasAttributeModifiers() ?
                    meta.getAttributeModifiers().get(Attribute.GENERIC_ATTACK_DAMAGE).stream()
                            .mapToDouble(att -> att.getAmount())
                            .sum() : 0;

            meta.removeAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE);
            meta.addAttributeModifier(Attribute.GENERIC_ATTACK_DAMAGE,
                    new org.bukkit.attribute.AttributeModifier(
                            UUID.randomUUID(),
                            damageKey,
                            currentDamage + damageBonus,
                            org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                            EquipmentSlot.HAND
                    ));

            player.sendMessage(ChatColor.GREEN + "武器强化成功! 基础伤害提升 " + damageBonus + " 点");
        }

        weapon.setItemMeta(meta);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    private void upgradeArmor(Player player, ItemStack armor, int stoneLevel) {
        ItemMeta meta = armor.getItemMeta();

        // Ⅰ级强化石：提升保护等级
        if (stoneLevel == 1) {
            int currentLevel = meta.getEnchantLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
            int newLevel = currentLevel + 1;

            if (newLevel > MAX_ENCHANT_LEVEL) {
                player.sendMessage(ChatColor.RED + "这件护甲的保护等级已经达到最大值 " + MAX_ENCHANT_LEVEL + "!");
                return;
            }

            meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, newLevel, true);
            player.sendMessage(ChatColor.GREEN + "护甲强化成功! 保护等级提升至 " + newLevel);
        }
        // Ⅱ级强化石：直接提升护甲值
        else {
            double armorBonus = 1.0; // 每次提升1点护甲值
            String armorKey = "armor_value";

            double currentArmor = meta.hasAttributeModifiers() ?
                    meta.getAttributeModifiers().get(Attribute.GENERIC_ARMOR).stream()
                            .mapToDouble(att -> att.getAmount())
                            .sum() : 0;

            meta.removeAttributeModifier(Attribute.GENERIC_ARMOR);
            meta.addAttributeModifier(Attribute.GENERIC_ARMOR,
                    new org.bukkit.attribute.AttributeModifier(
                            UUID.randomUUID(),
                            armorKey,
                            currentArmor + armorBonus,
                            org.bukkit.attribute.AttributeModifier.Operation.ADD_NUMBER,
                            getEquipmentSlot(armor.getType())
                    ));

            player.sendMessage(ChatColor.GREEN + "护甲强化成功! 护甲值提升 " + armorBonus + " 点");
        }

        armor.setItemMeta(meta);
        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1.0f, 1.0f);
    }

    private EquipmentSlot getEquipmentSlot(Material material) {
        if (material.name().endsWith("_HELMET")) return EquipmentSlot.HEAD;
        if (material.name().endsWith("_CHESTPLATE")) return EquipmentSlot.CHEST;
        if (material.name().endsWith("_LEGGINGS")) return EquipmentSlot.LEGS;
        if (material.name().endsWith("_BOOTS")) return EquipmentSlot.FEET;
        return EquipmentSlot.HAND;
    }

    private int getUpgradeStoneType(ItemStack item) {
        if (item == null || !item.hasItemMeta()) return 0;

        String name = item.getItemMeta().getDisplayName();
        if (name.equals(WEAPON_STONE_I_NAME) || name.equals(ARMOR_STONE_I_NAME)) return 1;
        if (name.equals(WEAPON_STONE_II_NAME) || name.equals(ARMOR_STONE_II_NAME)) return 2;
        return 0;
    }

    private ItemStack createUpgradeStone(int level, boolean isWeaponStone) {
        Material material = level == 1 ?
                (isWeaponStone ? Material.EMERALD : Material.DIAMOND) :
                (isWeaponStone ? Material.NETHERITE_SCRAP : Material.NETHERITE_INGOT);

        ItemStack stone = new ItemStack(material);
        ItemMeta meta = stone.getItemMeta();

        meta.setDisplayName(isWeaponStone ?
                (level == 1 ? WEAPON_STONE_I_NAME : WEAPON_STONE_II_NAME) :
                (level == 1 ? ARMOR_STONE_I_NAME : ARMOR_STONE_II_NAME));

        meta.setLore(STONE_LORE);
        stone.setItemMeta(meta);
        return stone;
    }

    private static final int MAX_ENCHANT_LEVEL = 100;
    private static final double IRON_ZOMBIE_CHANCE = 0.05;
    private static final double DIAMOND_ZOMBIE_CHANCE = 0.02;

    private final Random random = new Random();
    private FileConfiguration config;
    private final Set<UUID> specialMobs = new HashSet<>();
    private final Map<UUID, BukkitRunnable> despawnTasks = new HashMap<>();

    @Override
    public void onEnable() {
        loadConfig();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("终极强化插件已启用!");
    }

    private void loadConfig() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            saveResource("config.yml", false);
        }
        config = YamlConfiguration.loadConfiguration(configFile);
        config.addDefault("despawn.enabled", true);
        config.addDefault("despawn.time", 1440);
        config.options().copyDefaults(true);
        try {
            config.save(configFile);
        } catch (IOException e) {
            getLogger().warning("无法保存配置文件: " + e.getMessage());
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!cmd.getName().equalsIgnoreCase("qianghua")) return false;
        if (!sender.hasPermission("weaponupgrade.admin")) {
            sender.sendMessage(ChatColor.RED + "你没有权限使用此命令!");
            return true;
        }

        if (args.length == 0) {
            sendHelpMessage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "give":
                return handleGiveCommand(sender, args);
            case "set":
                return handleSetCommand(sender, args);
            case "spawn":
                return handleSpawnCommand(sender);
            case "spawnkill":
                return handleSpawnKillCommand(sender);
            default:
                sendHelpMessage(sender);
                return true;
        }
    }

    private void sendHelpMessage(CommandSender sender) {
        sender.sendMessage(ChatColor.GOLD + "=== 终极强化插件指令帮助 ===");
        sender.sendMessage(ChatColor.YELLOW + "/qianghua give <武器Ⅰ/武器Ⅱ/护甲Ⅰ/护甲Ⅱ> <数量> - 获得强化石");
        sender.sendMessage(ChatColor.YELLOW + "/qianghua set <等级> - 设置手持装备的附魔等级");
        sender.sendMessage(ChatColor.YELLOW + "/qianghua spawn - 生成特殊怪物");
        sender.sendMessage(ChatColor.YELLOW + "/qianghua spawnkill - 清除所有特殊怪物");
    }

    private boolean handleGiveCommand(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "用法: /qianghua give <武器Ⅰ/武器Ⅱ/护甲Ⅰ/护甲Ⅱ> <数量>");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;
        String type = args[1].toLowerCase();
        int amount;

        try {
            amount = Integer.parseInt(args[2]);
            if (amount <= 0 || amount > 64) {
                sender.sendMessage(ChatColor.RED + "数量必须在1-64之间!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "请输入有效的数量!");
            return true;
        }

        ItemStack stone;
        switch (type) {
            case "武器Ⅰ":
                stone = createUpgradeStone(1, true);
                break;
            case "武器Ⅱ":
                stone = createUpgradeStone(2, true);
                break;
            case "护甲Ⅰ":
                stone = createUpgradeStone(1, false);
                break;
            case "护甲Ⅱ":
                stone = createUpgradeStone(2, false);
                break;
            default:
                sender.sendMessage(ChatColor.RED + "类型必须是'武器Ⅰ/武器Ⅱ/护甲Ⅰ/护甲Ⅱ'!");
                return true;
        }

        stone.setAmount(amount);
        player.getInventory().addItem(stone);
        sender.sendMessage(ChatColor.GREEN + "已获得 " + amount + " 个" + type);
        return true;
    }

    private boolean handleSetCommand(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "用法: /qianghua set <等级>");
            return true;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;
        ItemStack item = player.getInventory().getItemInMainHand();

        if (item == null || item.getType() == Material.AIR) {
            sender.sendMessage(ChatColor.RED + "请手持一件装备!");
            return true;
        }

        int level;
        try {
            level = Integer.parseInt(args[1]);
            if (level < 0 || level > MAX_ENCHANT_LEVEL) {
                sender.sendMessage(ChatColor.RED + "等级必须在0-" + MAX_ENCHANT_LEVEL + "之间!");
                return true;
            }
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.RED + "请输入有效的等级!");
            return true;
        }

        ItemMeta meta = item.getItemMeta();
        if (isWeapon(item.getType())) {
            meta.addEnchant(Enchantment.DAMAGE_ALL, level, true);
            sender.sendMessage(ChatColor.GREEN + "已将武器的锋利等级设置为 " + level);
        } else if (isArmor(item.getType())) {
            meta.addEnchant(Enchantment.PROTECTION_ENVIRONMENTAL, level, true);
            sender.sendMessage(ChatColor.GREEN + "已将护甲的保护等级设置为 " + level);
        } else {
            sender.sendMessage(ChatColor.RED + "手持的物品不是武器或护甲!");
            return true;
        }

        item.setItemMeta(meta);
        return true;
    }

    private boolean handleSpawnCommand(CommandSender sender) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "只有玩家可以使用此命令!");
            return true;
        }

        Player player = (Player) sender;
        Location loc = player.getLocation();

        // 生成铁套僵尸
        Zombie ironZombie = (Zombie) player.getWorld().spawnEntity(loc, EntityType.ZOMBIE);
        equipIronArmor(ironZombie);
        specialMobs.add(ironZombie.getUniqueId());
        scheduleDespawn(ironZombie);

        // 生成钻套骷髅
        Skeleton diamondSkeleton = (Skeleton) player.getWorld().spawnEntity(loc, EntityType.SKELETON);
        equipDiamondArmor(diamondSkeleton);
        specialMobs.add(diamondSkeleton.getUniqueId());
        scheduleDespawn(diamondSkeleton);

        player.sendMessage(ChatColor.GREEN + "已生成特殊怪物!");
        return true;
    }

    private boolean handleSpawnKillCommand(CommandSender sender) {
        int count = 0;
        for (UUID uuid : new HashSet<>(specialMobs)) {
            Entity entity = getServer().getEntity(uuid);
            if (entity != null) {
                entity.remove();
                count++;
            }
        }
        specialMobs.clear();
        sender.sendMessage(ChatColor.GREEN + "已清除 " + count + " 个特殊怪物");
        return true;
    }

    // 其他方法保持不变...

    private boolean isWeapon(Material material) {
        return material.name().endsWith("_SWORD") ||
                material.name().endsWith("_AXE") ||
                material == Material.TRIDENT;
    }

    private boolean isArmor(Material material) {
        return material.name().endsWith("_HELMET") ||
                material.name().endsWith("_CHESTPLATE") ||
                material.name().endsWith("_LEGGINGS") ||
                material.name().endsWith("_BOOTS");
    }
}