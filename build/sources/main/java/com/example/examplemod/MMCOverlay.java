package com.example.examplemod;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.gui.ScaledResolution;
import net.minecraft.client.gui.inventory.GuiChest;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.ClientCommandHandler;
import net.minecraftforge.client.event.RenderGameOverlayEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.minecraft.inventory.Slot;
import net.minecraftforge.client.event.ClientChatReceivedEvent;
import net.minecraftforge.client.event.GuiScreenEvent;
import org.lwjgl.input.Keyboard;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.Optional;




@Mod(modid = "mmcoverlay", name = "mmcoverlay ", version = "1.0")
public class MMCOverlay extends GuiScreen {

    private static final Minecraft mc = Minecraft.getMinecraft();
    private boolean guiOpened = true;
    private static int wslimit = 10;

    public static void setWSLimit(int limit) {
        wslimit = limit;
    }

    private boolean handledLowWS = false;

    public static String opponentName = "";
    private String GuiGamemode = "";
    private String sanitizedOpponentName = "";

    private String currentgamemode = "";
    static List<String> unformattedLines = new ArrayList<>();
    private boolean CanExtract = false;
    private static Map<String, String> stats = new LinkedHashMap<>();


    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        ClientCommandHandler.instance.registerCommand(new CommandWSLimit());
        MinecraftForge.EVENT_BUS.register(this);
    }

    private Optional<IInventory> getLowerInventory(GuiChest chest) {
        try {
            Field f = chest.getClass().getDeclaredField("field_147015_w");
            f.setAccessible(true);
            return Optional.ofNullable((IInventory) f.get(chest));
        } catch (NoSuchFieldException | IllegalAccessException e) {
            return Optional.empty();
        }
    }



    @SubscribeEvent
    public void onTick(TickEvent.ClientTickEvent event) {
        if (CanExtract) {
            GuiGamemode = currentgamemode;
        }

        if (mc.thePlayer == null || mc.theWorld == null)
            return;

        if (mc.currentScreen instanceof GuiChest && CanExtract) {
            if (!guiOpened) {
                extractSlotData();
                guiOpened = true;
            }
        } else {
            guiOpened = false;
        }
        if (!handledLowWS && stats.containsKey("Win Streak")) {
            try {
                int winstreak = Integer.parseInt(stats.get("Win Streak"));

                if (winstreak < wslimit) {
                    handledLowWS = true;
                    new Thread(() -> {
                        try {
                            Thread.sleep(1500);
                            mc.thePlayer.sendChatMessage("/l");

                            Thread.sleep(5000);
                            mc.thePlayer.sendChatMessage("/requeue");
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }).start();
                } else if (winstreak >= wslimit) {
                    handledLowWS = true;
                    mc.thePlayer.playSound("random.orb", 1.0F, 1.0F);
                }
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }

    private void extractSlotData() {
        GuiChest chest = (GuiChest) mc.currentScreen;
        Optional<IInventory> lowerInventoryOpt = getLowerInventory(chest);

        if (lowerInventoryOpt.isPresent()) {
            IInventory lowerInventory = lowerInventoryOpt.get();
            boolean foundItem = false;

            if (lowerInventory.getDisplayName().getUnformattedText().contains("Stats")) {
                for (int slotIndex = 0; slotIndex < lowerInventory.getSizeInventory(); slotIndex++) {
                    ItemStack itemStack = lowerInventory.getStackInSlot(slotIndex);

                    if (itemStack != null && itemStack.stackSize > 0) {
                        String itemName = itemStack.getDisplayName();

                        if (itemName.contains(currentgamemode)) {
                            foundItem = true;
                            List<String> tooltip = itemStack.getTooltip(mc.thePlayer, mc.gameSettings.advancedItemTooltips);
                            unformattedLines.clear();
                            stats.clear();

                            for (String line : tooltip) {
                                String unformattedLine = new ChatComponentText(line).getUnformattedText().trim();
                                String sanitizedLine = unformattedLine.replaceAll("[\u00A7\u200B-\u200F\u202A-\u202E].|\u272B", "");
                                unformattedLines.add(sanitizedLine);

                                Matcher streakMatch = Pattern.compile("Win Streak:\\s*(\\d+)").matcher(sanitizedLine);
                                if (streakMatch.find()) {
                                    stats.put("Win Streak", streakMatch.group(1));
                                    break;
                                }
                            }

                            CanExtract = false;
                            handledLowWS = false;
                            closeGui();
                            return;
                        }
                    }
                }

                if (!foundItem) {
                    stats.clear();
                    stats.put("Win Streak", "0");
                    CanExtract = false;
                    handledLowWS = false;
                    closeGui();
                }
            }
        }
    }




    @SubscribeEvent
    public void onRenderGameOverlay(RenderGameOverlayEvent.Text event) {
        ScaledResolution sr = new ScaledResolution(mc);

        int x = 10;
        int y = 10;
        int winstreakY = y + 12;

        String title = sanitizedOpponentName.isEmpty() ? "mmcOverlay" : sanitizedOpponentName;
        Color borderColor = new Color(234, 17, 230);

        mc.fontRendererObj.drawStringWithShadow(title, x, y, borderColor.getRGB());


        String winstreakValue = stats.get("Win Streak");
        if (winstreakValue != null) {
            int winstreak = Integer.parseInt(winstreakValue);
            Color winstreakColor = Color.GRAY;

            if (winstreak >= 7 && winstreak <= 19) {
                winstreakColor = new Color(255, 255, 0);
            } else if (winstreak >= 20 && winstreak <= 39) {
                winstreakColor = new Color(255, 0, 0);
            } else if (winstreak >= 40 && winstreak <= 69) {
                winstreakColor = new Color(139, 0, 0);
            } else if (winstreak >= 70) {
                winstreakColor = new Color(216, 0, 244);
            }


            mc.fontRendererObj.drawStringWithShadow("winstreak: ", x, winstreakY, Color.WHITE.getRGB());
            mc.fontRendererObj.drawStringWithShadow(winstreakValue, x + mc.fontRendererObj.getStringWidth("winstreak: "), winstreakY, winstreakColor.getRGB());

        }

        ResourceLocation logo = new ResourceLocation("wssniper", "textures/gui/result.png");
        mc.getTextureManager().bindTexture(logo);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.color(1F, 1F, 1F, 0.7F);

        int imageSize = 50;
        int logoY = winstreakY - 8;
        drawModalRectWithCustomSizedTexture(x - 18, logoY, 0, 0, imageSize, imageSize, imageSize, imageSize);

        GlStateManager.popMatrix();
        GlStateManager.disableBlend();
    }


    public void setGameMode(String gameMode) {
        this.currentgamemode = gameMode;
    }

    @SubscribeEvent
    public void onChatReceived(ClientChatReceivedEvent event) {

        String message = event.message.getUnformattedText();
        if (message.contains("Opponent: ")) {
            String opponentInfo = message.substring(message.indexOf("Opponent: ") + 10).trim();
            int rankStart = opponentInfo.indexOf(" (");

            if (rankStart != -1 && opponentInfo.endsWith(")")) {
                opponentName = opponentInfo.substring(0, rankStart).trim();
            } else {
                opponentName = opponentInfo.trim();
            }

            sanitizedOpponentName = opponentName.replaceAll("[\u00A7\u200B-\u200F\u202A-\u202E].", "");
            new Thread(() -> {
                try {
                    Minecraft.getMinecraft().thePlayer.sendChatMessage("/stats " + sanitizedOpponentName);
                    Thread.sleep(100);
                    CanExtract = true;
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        }
    }

    private void closeGui() {
        try {
            if (mc.currentScreen != null) {
                mc.displayGuiScreen(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @SubscribeEvent
    public void onMouseClick(GuiScreenEvent.MouseInputEvent.Pre event) {
        try {
            if (!(mc.currentScreen instanceof GuiChest)) {
                return;
            }

            if (Mouse.getEventButton() == 0 && Mouse.getEventButtonState()) {
                GuiChest guiContainer = (GuiChest) mc.currentScreen;
                Slot slot = guiContainer.getSlotUnderMouse();

                if (slot == null || !slot.getHasStack()) {
                    return;
                }

                ItemStack clickedItem = slot.getStack();

                if (clickedItem != null) {
                    String itemName = clickedItem.getDisplayName();
                    if (itemName != null) {

                        if (itemName.contains("Bed Fight")) {
                            setGameMode("Bed Fight");
                        } else if (itemName.contains("Sumo") && !itemName.contains("TNT")) {
                            setGameMode("Sumo");
                        } else if (itemName.contains("Boxing")) {
                            setGameMode("Boxing");
                        } else if (itemName.contains("Bridges")) {
                            setGameMode("Bridges");
                        } else if (itemName.contains("Top Fight")) {
                            setGameMode("Top Fight");
                        } else if (itemName.contains("Vanilla")) {
                            setGameMode("Vanilla");
                        } else if (itemName.contains("Build UHC")) {
                            setGameMode("Build UHC");
                        } else if (itemName.contains("Battle Rush")) {
                            setGameMode("Battle Rush");
                        } else if (itemName.contains("Pearl Fight")) {
                            setGameMode("Pearl Fight");
                        } else if (itemName.contains("Final UHC")) {
                            setGameMode("Final UHC");
                        } else if (itemName.contains("Classic")) {
                            setGameMode("Classic");
                        } else if (itemName.contains("Invaded")) {
                            setGameMode("Invaded");
                        } else if (itemName.contains("Gapple")) {
                            setGameMode("Gapple");
                        } else if (itemName.contains("Emerald Rush")) {
                            setGameMode("Emerald Rush");
                        } else if (itemName.contains("Stick Fight")) {
                            setGameMode("Stick Fight");
                        } else if (itemName.contains("TNT Sumo")) {
                            setGameMode("TNT Sumo");
                        } else if (itemName.contains("Sky Wars")) {
                            setGameMode("Sky Wars");
                        } else if (itemName.contains("HCF")) {
                            setGameMode("HCF");
                        } else if (itemName.contains("Parkour")) {
                            setGameMode("Parkour");
                        } else if (itemName.contains("No Debuff")) {
                            setGameMode("No Debuff");
                        } else if (itemName.contains("Fireball Fight")) {
                            setGameMode("Fireball Fight");
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

