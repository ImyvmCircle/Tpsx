package com.imyvm.tpsx;

import static org.junit.Assert.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.mockito.AdditionalAnswers;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.reflect.Whitebox;

import com.connorlinfoot.actionbarapi.ActionBarAPI;
import net.minecraft.server.v1_14_R1.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ActionBarAPI.class, MinecraftServer.class, Bukkit.class})
class TpsxTest {
    private static MinecraftServer mockedServer = mock(MinecraftServer.class);
    private BukkitScheduler mockedScheduler;
    private Command mockedCommand;
    private Timer timer;
    private Tpsx plugin;

    public TpsxTest() {}

    @BeforeClass
    static public void setUpClass() {
        setMsptData(mockedServer, 0);
    }

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() {
        PowerMockito.mockStatic(ActionBarAPI.class);
        PowerMockito.mockStatic(MinecraftServer.class);
        PowerMockito.mockStatic(Bukkit.class);

        setMsptData(mockedServer, 0);

        mockedScheduler = mock(BukkitScheduler.class);
        doAnswer(new Answer<Integer>() {
            @Override
            public Integer answer(InvocationOnMock invocation) {
                Object[] args = invocation.getArguments();
                Runnable runnable = (Runnable)args[1];

                // times 50 to convert ticks to milliseconds, and divide 10 to reduce test time
                long delay = (long)args[2] * 50 / 10;
                long period = (long)args[3] * 50 / 10;

                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        runnable.run();
                    }
                }, delay, period);

                return 0;
            }
        }).when(mockedScheduler).scheduleSyncRepeatingTask(any(Tpsx.class), any(Runnable.class), anyLong(), anyLong());

        when(Bukkit.getScheduler()).thenReturn(mockedScheduler);
        when(MinecraftServer.getServer()).thenReturn(mockedServer);

        mockedCommand = mock(Command.class);
        when(mockedCommand.getName()).thenReturn("tpsx");

        timer = new Timer();

        plugin = mock(Tpsx.class);
        doCallRealMethod().when(plugin).onEnable();
        doCallRealMethod().when(plugin).sendTpsInfo();
        doCallRealMethod().when(plugin).onCommand(anyObject(), anyObject(), anyString(), anyObject());

        plugin.onEnable();
    }

    @After
    public void tearDown() {
        timer.cancel();
        plugin.onDisable();
    }

    static void setMsptData(MinecraftServer server, long mspt) {
        long[] array = new long[100];
        for (int i = 0; i < array.length; i++) {
            array[i] = mspt * 1000000;
        }
        Whitebox.setInternalState(server, "f", array);
    }

    @Test
    public void testMainFunction() {
        Player mockedPlayer = mock(Player.class);
        when(mockedPlayer.getUniqueId()).thenReturn(new UUID(1, 1));

        setMsptData(mockedServer, 10);

        assertTrue(plugin.onCommand(mockedPlayer, mockedCommand, "", new String[0]));
        verify(mockedPlayer, times(1)).sendMessage(eq("tpsx on"));

        PowerMockito.verifyStatic(ActionBarAPI.class, timeout(1200 / 10).atLeast(1));
        ActionBarAPI.sendActionBar(eq(mockedPlayer), eq("TPS: §a20.00§r, MSPT: §a10.00§r"));
    }

    @Test
    public void testTpsColorAndData() {
        try {
            Method method = Tpsx.class.getDeclaredMethod("getTpsInfo");
            method.setAccessible(true);

            setMsptData(mockedServer, 10);
            assertEquals(method.invoke(plugin), "TPS: §a20.00§r, MSPT: §a10.00§r");

            setMsptData(mockedServer, 45);
            assertEquals(method.invoke(plugin), "TPS: §a20.00§r, MSPT: §e45.00§r");

            setMsptData(mockedServer, 55);
            assertEquals(method.invoke(plugin), "TPS: §c18.18§r, MSPT: §e55.00§r");

            setMsptData(mockedServer, 80);
            assertEquals(method.invoke(plugin), "TPS: §c12.50§r, MSPT: §c80.00§r");
        }
        catch (NoSuchMethodException e) {
            e.printStackTrace();
            fail();
        }
        catch (IllegalAccessException e) {
            e.printStackTrace();
            fail();
        }
        catch (InvocationTargetException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testCommandFromConsole() {
        CommandSender mockedSender = mock(CommandSender.class);

        setMsptData(mockedServer, 10);
        plugin.onCommand(mockedSender, mockedCommand, "", new String[0]);
        verify(mockedSender, times(1)).sendMessage(eq("TPS: §a20.00§r, MSPT: §a10.00§r"));
    }

    @Test
    public void testTurnOffTpsInfo() {
        Player mockedPlayer = mock(Player.class);
        when(mockedPlayer.getUniqueId()).thenReturn(new UUID(2, 2));

        setMsptData(mockedServer, 10);

        plugin.onCommand(mockedPlayer, mockedCommand, "", new String[0]);
        verify(mockedPlayer, times(1)).sendMessage(eq("tpsx on"));

        PowerMockito.verifyStatic(ActionBarAPI.class, timeout(1200 / 10).atLeast(1));
        ActionBarAPI.sendActionBar(eq(mockedPlayer), eq("TPS: §a20.00§r, MSPT: §a10.00§r"));

        plugin.onCommand(mockedPlayer, mockedCommand, "", new String[0]);
        verify(mockedPlayer, times(1)).sendMessage(eq("tpsx off"));
        PowerMockito.verifyStatic(ActionBarAPI.class, after(2400 / 10).atMost(2));
        ActionBarAPI.sendActionBar(eq(mockedPlayer), anyString());
    }
}
