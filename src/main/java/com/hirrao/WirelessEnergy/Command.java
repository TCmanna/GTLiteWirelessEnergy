package com.hirrao.WirelessEnergy;

import net.minecraft.command.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.world.World;

import java.math.BigInteger;
import java.util.UUID;

import static com.hirrao.WirelessEnergy.EventTick.totalEnergyLast;
import static com.hirrao.WirelessEnergy.EventTick.totalEnergyNow;

public class Command extends CommandBase {

    @Override
    public String getName() {
        return "energy";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "/energy [user]";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        World world = sender.getEntityWorld();
        if (world.isRemote) {
            sender.sendMessage(new TextComponentString("This command can only be run on the server."));
            return;
        }

        EntityPlayer player;

        if (args.length < 1) {
            if (sender instanceof EntityPlayerMP) player = (EntityPlayerMP) sender;
            else {
                sender.sendMessage(new TextComponentString("No player found"));
                return;
            }
        } else {
            String playerName = args[0];
            try {
                player = getPlayer(server, sender, playerName);
            } catch (PlayerNotFoundException e) {
                sender.sendMessage(new TextComponentString("No player found with name: " + playerName));
                return;
            }
        }
        UUID uuid = player.getUniqueID();
        try {
            BigInteger energyNow = totalEnergyNow.getOrDefault(uuid, BigInteger.ZERO);
            BigInteger energyLast = totalEnergyLast.getOrDefault(uuid, BigInteger.ZERO);
            BigInteger energyDiff = energyNow.subtract(energyLast);
            Log.LOGGER.info("energyNow: " + energyNow + ", energyLast: " + energyLast + ", energyDiff: " + energyDiff);
            sender.sendMessage(new TextComponentString("无线电网存储能量 " + String.format("%,d", energyNow) + " EU"));
            if (energyDiff.compareTo(BigInteger.ZERO) >= 0) {
                sender.sendMessage(new TextComponentString("平均输入" + String.format("%,d", energyDiff.divide(BigInteger.valueOf(20))) + " EU/t"));
            } else if (energyDiff.compareTo(BigInteger.ZERO) < 0) {
                sender.sendMessage(new TextComponentString("平均输出" + String.format("%,d", energyDiff.abs().divide(BigInteger.valueOf(20))) + " EU/t"));
                BigInteger time = energyNow.divide(energyDiff.abs());
                sender.sendMessage(new TextComponentString("预计" + formatDuration(time) + "后能量耗尽"));
            }
        } catch (Exception e) {
            Log.LOGGER.error(e);
        }

    }

    private static String formatDuration(BigInteger time) {
        BigInteger[] dr = time.divideAndRemainder(BigInteger.valueOf(86400));
        BigInteger days = dr[0];
        BigInteger[] hr = dr[1].divideAndRemainder(BigInteger.valueOf(3600));
        BigInteger hours = hr[0];
        BigInteger[] mr = hr[1].divideAndRemainder(BigInteger.valueOf(60));
        BigInteger minutes = mr[0];
        BigInteger seconds = mr[1];
        StringBuilder sb = new StringBuilder();

        if (days.signum() > 0) sb.append(days).append("天");
        if (hours.signum() > 0) sb.append(hours).append("小时");
        if (minutes.signum() > 0) sb.append(minutes).append("分钟");
        sb.append(seconds).append("秒");

        return sb.toString();
    }
}
