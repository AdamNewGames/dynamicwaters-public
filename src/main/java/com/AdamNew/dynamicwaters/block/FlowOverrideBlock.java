package com.AdamNew.dynamicwaters.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.AirBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

public class FlowOverrideBlock extends AirBlock {
    public static final IntegerProperty FLOW_DIRECTION = IntegerProperty.create("flow_direction", 0, 15);

    public static final BooleanProperty FLOW_ENABLED = BooleanProperty.create("flow_enabled");

    public static final IntegerProperty FLOW_SPEED = IntegerProperty.create("flow_speed", 0, 7);

    public static final double[] SPEEDS = {0.25, 0.5, 0.75, 1.0, 1.5, 2.0, 2.5, 3.0};

    public static final int DEFAULT_SPEED = 3;

    public FlowOverrideBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FLOW_DIRECTION, 0)
                .setValue(FLOW_ENABLED, true)
                .setValue(FLOW_SPEED, DEFAULT_SPEED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLOW_DIRECTION, FLOW_ENABLED, FLOW_SPEED);
    }

    public static double pace(int step) {
        return SPEEDS[step >= 0 && step < SPEEDS.length ? step : DEFAULT_SPEED];
    }

    public static Vec3 flowOf(BlockState override) {
        return directionToVec3(override.getValue(FLOW_DIRECTION)).scale(pace(override.getValue(FLOW_SPEED)));
    }

    public static String speedText(double speed) {
        return java.math.BigDecimal.valueOf(speed).stripTrailingZeros().toPlainString();
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return Shapes.empty();
    }

    @Override
    public boolean canBeReplaced(BlockState state, BlockPlaceContext useContext) {
        return true;
    }

    @Override
    public BlockState updateShape(BlockState stateHere, Direction direction, BlockState neighborState,
                                  LevelAccessor level, BlockPos here, BlockPos neighborPos) {
        if (direction == Direction.DOWN) {
            if (neighborState.getFluidState().isEmpty()) {
                return net.minecraft.world.level.block.Blocks.AIR.defaultBlockState();
            }
        }
        return stateHere;
    }

    public static Vec3 directionToVec3(int direction) {
        double yawDeg = direction * 22.5;
        double yawRad = Math.toRadians(yawDeg);

        double x = -Math.sin(yawRad);
        double z =  Math.cos(yawRad);
        return new Vec3(x, 0, z);
    }

    public static int vec3ToDirection(double dx, double dz) {
        double yawRad = Math.atan2(-dx, dz);
        double yawDeg = Math.toDegrees(yawRad);
        if (yawDeg < 0) yawDeg += 360.0;
        int segment = (int) Math.round(yawDeg / 22.5) % 16;
        return segment;
    }
}
