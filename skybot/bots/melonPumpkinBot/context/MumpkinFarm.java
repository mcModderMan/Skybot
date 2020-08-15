package inowen.skybot.bots.melonPumpkinBot.context;

import inowen.skybot.utils.FarmZoneConstraints;
import inowen.utils.CoordinateTranslator;
import net.minecraft.block.*;
import net.minecraft.client.Minecraft;
import net.minecraft.item.Item;
import net.minecraft.item.Items;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

/**
 * Contains all the information about the physical farm ingame.
 *
 * @author inowen
 */
public class MumpkinFarm {

    private static Minecraft mc = Minecraft.getInstance();
    // The kind of block with which the farm enclosure is made.
    public static final Block BARRIER_BLOCK = Blocks.COBBLESTONE;
    public static final Item itemBeingFarmed = Items.MELON;

    public FarmZoneConstraints zoneConstraints = null;
    public FarmSlot[][] farmSlots = null;


    /**
     * Load the farm in. Stop if no farm found.
     */
    public void init(MumpkinInitTracker tracker) {
        // Find the farm borders.
        zoneConstraints = new FarmZoneConstraints(BARRIER_BLOCK);
        zoneConstraints.findCorners(new BlockPos(mc.player.getPositionVector()), tracker);

        if (tracker.foundFarmConstraints) {
            // Create the FarmSlot matrix (x, z)
            farmSlots = new FarmSlot[zoneConstraints.lengthXAxis()][zoneConstraints.lengthZAxis()];
            // Get the contents of those farmslots for the first time.
            updateFarmSlots();
        }

    }

    /**
     * Update all information in the farm to use it in this tick.
     */
    public void update() {
        if(mc.player != null && mc.world != null) {
            updateFarmSlots();
        }
    }

    /**
     * Recalculate content for the FarmSlot matrix.
     */
    public void updateFarmSlots() {
        BlockPos minimalPosition = new BlockPos(zoneConstraints.minX, zoneConstraints.yLevel, zoneConstraints.minZ);

        // Go through the entire farm and set the values in the matrix.
        for (int x=0; x<zoneConstraints.lengthXAxis(); x++) {
            for (int z=0; z<zoneConstraints.lengthZAxis(); z++) {
                FarmSlot thisSlot = new FarmSlot();
                thisSlot.globalBlockPos = new BlockPos(minimalPosition.getX()+x, minimalPosition.getY(), minimalPosition.getZ()+z);

                Block blockAtThisSlot = mc.world.getBlockState(thisSlot.globalBlockPos).getBlock();
                if (blockAtThisSlot instanceof SlabBlock) {
                    thisSlot.content = FarmSlot.FarmSlotContent.COVERS_WATER;
                }
                else if (blockAtThisSlot instanceof AirBlock) {
                    thisSlot.content = FarmSlot.FarmSlotContent.EMPTY;
                }
                else if (blockAtThisSlot instanceof MelonBlock) {
                    thisSlot.content = FarmSlot.FarmSlotContent.MELON_BLOCK;
                }
                else if (blockAtThisSlot instanceof StemBlock || blockAtThisSlot instanceof StemGrownBlock) {
                    thisSlot.content = FarmSlot.FarmSlotContent.MELON_STEM;
                }
                else {
                    thisSlot.content = FarmSlot.FarmSlotContent.UNKNOWN;
                }

                farmSlots[x][z] = thisSlot;
            }
        }

    }


    /**
     * Map of the farm as a list of strings (one per line).
     * Displaying the strings top-down, they make a map of the farm from the FarmZoneContents.
     * @return
     */
    public ArrayList<String> getStringMap() {
        ArrayList<String> map = new ArrayList<>();

        for (int x=0; x<zoneConstraints.lengthXAxis(); x++) {
            // Add the rows one by one.
            String currentRow = "";
            for (int z=0; z<zoneConstraints.lengthZAxis(); z++) {
                FarmSlot.FarmSlotContent slotContent = farmSlots[x][z].content;
                if (slotContent == FarmSlot.FarmSlotContent.EMPTY) {
                    currentRow += "E ";
                }
                else if (slotContent == FarmSlot.FarmSlotContent.COVERS_WATER) {
                    currentRow += "W ";
                }
                else if (slotContent == FarmSlot.FarmSlotContent.MELON_BLOCK) {
                    currentRow += "M ";
                }
                else if (slotContent == FarmSlot.FarmSlotContent.MELON_STEM) {
                    currentRow += "S ";
                }
                else if (slotContent == FarmSlot.FarmSlotContent.UNKNOWN) {
                    currentRow += "? ";
                }
                else {
                    currentRow += "# ";
                }
            }

            map.add(currentRow);
        }

        return map;
    }


    /**
     * Count how many melon blocks there are in the farm.
     * (probably mostly to know if there are 0 or more).
     * @return int
     */
    public int numFullyGrownMelons() {
        int totalNum = 0;
        for (int x=0; x<zoneConstraints.lengthXAxis(); x++) {
            for (int z=0; z<zoneConstraints.lengthZAxis(); z++) {
                if (farmSlots[x][z].content == FarmSlot.FarmSlotContent.MELON_BLOCK) {
                    totalNum++;
                }
            }
        }
        return totalNum;
    }


    /**
     * Find the position of the closest block matching the queried content.
     * If there are no such blocks, it will return null.
     * @param reference Position to which it should be closest.
     * @param farmSlotContent Queried content.
     * @return BlockPos
     */
    public BlockPos posClosestMatching(Vec3d reference, FarmSlot.FarmSlotContent farmSlotContent) {
        BlockPos result = null;
        double closestDistance = 999999D;

        // Go through all the slots in the farm
        for (int x=0; x<zoneConstraints.lengthXAxis(); x++) {
            for (int z=0; z<zoneConstraints.lengthZAxis(); z++) {

                // Check if the block matches the queried content.
                FarmSlot currentSlot = farmSlots[x][z];
                if (currentSlot.content == farmSlotContent) {

                    // Calculate distance to the reference position
                    Vec3d refPosToTarget = CoordinateTranslator.blockPosToVectorPosition(currentSlot.globalBlockPos).subtract(reference);
                    Vec3d refPosToTargetXZ = new Vec3d(refPosToTarget.getX(), 0, refPosToTarget.getZ());
                    double currentDistance = refPosToTargetXZ.length();

                    // If it meets the criteria, make it the new closest.
                    if (currentDistance <= closestDistance) {
                        closestDistance = currentDistance;
                        result = currentSlot.globalBlockPos;
                    }
                }
            }
        }

        return result;
    }


}
