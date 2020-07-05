package com.imyvm.tpsx;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_16_R1.entity.CraftPlayer;
import org.bukkit.event.Listener;
import org.bukkit.plugin.PluginManager;
import sun.misc.Unsafe;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.Test;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.connorlinfoot.actionbarapi.ActionBarAPI;
import net.minecraft.server.v1_16_R1.MinecraftServer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ActionBarAPI.class, MinecraftServer.class, Bukkit.class, PluginCommand.class, Tpsx.class})
@PowerMockIgnore({"com.sun.org.apache.xerces.*", "javax.xml.*", "org.xml.*", "org.w3c.dom.*"})
class TpsxTest {
    // regex copied from MiniHUD
    private static final Pattern PATTERN_CARPET_TPS = Pattern.compile("TPS: (?<tps>[0-9]+[\\.,][0-9]) MSPT: (?<mspt>[0-9]+[\\.,][0-9])");

    private static MinecraftServer mockedServer;
    private static Unsafe unsafe;
    private static Field serverMsptArrayField;
    private Command mockedCommand;
    private FileConfiguration mockedConfig;
    private Listener pluginListener;
    private PluginCommand mockedPluginCommand;
    private Timer timer;
    private Tpsx plugin;

    public TpsxTest() {}

    @BeforeClass
    static public void setUpClass() throws Exception {
        Field unsafeField = Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);
        unsafe = (Unsafe) unsafeField.get(null);

        for (Field field : MinecraftServer.class.getDeclaredFields()) {
            if (field.getName().equals("h")) {
                serverMsptArrayField = field;
                break;
            }
        }
        if (serverMsptArrayField == null) {
            fail("Could not found server mspt array field");
        }

        mockedServer = mock(MinecraftServer.class);
        setMsptData(mockedServer, 1);
    }

    @SuppressWarnings("deprecation")
    @Before
    public void setUp() throws Exception {
        PowerMockito.mockStatic(ActionBarAPI.class);
        PowerMockito.mockStatic(MinecraftServer.class);
        PowerMockito.mockStatic(Bukkit.class);

        setMsptData(mockedServer, 0);

        BukkitScheduler mockedScheduler = mock(BukkitScheduler.class);
        PluginManager mockedManager = mock(PluginManager.class);
        mockedPluginCommand = PowerMockito.mock(PluginCommand.class);
        mockedCommand = mock(Command.class);
        mockedConfig = mock(FileConfiguration.class);
        timer = new Timer(true);

        doAnswer(invocation -> pluginListener = invocation.getArgument(0))
                .when(mockedManager).registerEvents(any(Listener.class), any(Tpsx.class));

        doAnswer(invocation -> {
            Runnable runnable = (Runnable)invocation.getArgument(1);

            // times 50 to convert ticks to milliseconds, and divide 10 to reduce test time
            long delay = (long)invocation.getArgument(2) * 50 / 10;
            long period = (long)invocation.getArgument(3) * 50 / 10;

            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    runnable.run();
                }
            }, delay, period);

            return 0;
        }).when(mockedScheduler).scheduleSyncRepeatingTask(any(Tpsx.class), any(Runnable.class), anyLong(), anyLong());

        when(Bukkit.getScheduler()).thenReturn(mockedScheduler);
        when(Bukkit.getPluginManager()).thenReturn(mockedManager);
        when(MinecraftServer.getServer()).thenReturn(mockedServer);
        when(mockedCommand.getName()).thenReturn("tpsx");
        when(mockedConfig.getString(anyString())).thenReturn(null);

        plugin = PowerMockito.mock(Tpsx.class);
        doCallRealMethod().when(plugin).onEnable();
        doCallRealMethod().when(plugin).sendTpsInfo();
        doCallRealMethod().when(plugin).onCommand(anyObject(), anyObject(), anyString(), anyObject());
        setObjectField(Tpsx.class, plugin, "barPlayers", new HashMap<>());
        setObjectField(Tpsx.class, plugin, "tabPlayers", new HashMap<>());
        when(plugin.getCommand(eq("tpsx"))).thenReturn(mockedPluginCommand);
        when(plugin.getConfig()).thenReturn(mockedConfig);
        PowerMockito.doCallRealMethod().when(plugin, "subCommandToggle", any(), anyString());
        PowerMockito.doCallRealMethod().when(plugin, "switchTo", any(), anyString());
        PowerMockito.doCallRealMethod().when(plugin, "getTpsInfo");
        PowerMockito.doNothing().when(plugin, "setPlayerListFooter", any(), anyString());

        plugin.onEnable();
    }

    @After
    public void tearDown() {
        timer.cancel();
        plugin.onDisable();
    }

    private static void setObjectField(Class cls, Object object, String fieldName, Object value) {
        Field targetField = null;
        for (Field field : cls.getDeclaredFields()) {
            if (field.getName().equals(fieldName)) {
                targetField = field;
                break;
            }
        }
        if (targetField == null) {
            fail("No such field \"" + fieldName + "\"");
        }
        unsafe.putObject(object, unsafe.objectFieldOffset(targetField), value);
    }

    private static void setMsptData(MinecraftServer server, long mspt) {
        long[] array = new long[100];
        for (int i = 0; i < array.length; i++) {
            array[i] = mspt * 1000000;
        }
        unsafe.putObject(server, unsafe.objectFieldOffset(serverMsptArrayField), array);
    }

    private static String removeColorCode(String msg) {
        return msg.replaceAll("§.", "");
    }

    @Test
    public void testMainFunction() {
        Player mockedPlayer = mock(CraftPlayer.class);
        when(mockedPlayer.getUniqueId()).thenReturn(new UUID(1, 1));

        setMsptData(mockedServer, 10);

        assertTrue(plugin.onCommand(mockedPlayer, mockedCommand, "", new String[] { "toggle", "bar" }));

        PowerMockito.verifyStatic(ActionBarAPI.class, timeout(1200 / 10).atLeast(1));
        ActionBarAPI.sendActionBar(eq(mockedPlayer), argThat((String msg) -> removeColorCode(msg).equals("TPS: 20.0 MSPT: 10.0")));
    }

    @Test
    public void testTpsColorAndData() {
        try {
            Method method = Tpsx.class.getDeclaredMethod("getTpsInfo");
            method.setAccessible(true);

	    setMsptData(mockedServer, 10);
            assertEquals(method.invoke(plugin), "TPS: §a20.0§r MSPT: §a10.0§r");

            setMsptData(mockedServer, 45);
            assertEquals(method.invoke(plugin), "TPS: §a20.0§r MSPT: §e45.0§r");

            setMsptData(mockedServer, 55);
            assertEquals(method.invoke(plugin), "TPS: §c18.2§r MSPT: §e55.0§r");

            setMsptData(mockedServer, 80);
            assertEquals(method.invoke(plugin), "TPS: §c12.5§r MSPT: §c80.0§r");
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Fail with an exception");
        }
    }

    @Test
    public void testCommandFromConsole() {
        CommandSender mockedSender = mock(CommandSender.class);

        setMsptData(mockedServer, 10);
        plugin.onCommand(mockedSender, mockedCommand, "", new String[0]);
        verify(mockedSender, times(1)).sendMessage(argThat((String msg) -> removeColorCode(msg).equals("TPS: 20.0 MSPT: 10.0")));
    }

    @Test
    public void testCarpetFormattedTps() {
        try {
            Method method = Tpsx.class.getDeclaredMethod("getTpsInfo");
            method.setAccessible(true);

            setMsptData(mockedServer, 10);

            String text = removeColorCode((String)method.invoke(plugin));
            assertTrue(PATTERN_CARPET_TPS.matcher(text).matches());
        }
        catch (Exception e) {
            e.printStackTrace();
            fail("Fail with an exception");
        }
    }
}
