package cz.boosik.boosCooldown;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import util.boosChat;

public class boosCoolDownManager {

	private static YamlConfiguration confusers;
	private static File confFile;

	public boosCoolDownManager(boosCoolDown bCoolDown) {
		confFile = new File(bCoolDown.getDataFolder(), "users.yml");

		confusers = new YamlConfiguration();

		if (confFile.exists()) {
			try {
				confusers.load(confFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InvalidConfigurationException e) {
				e.printStackTrace();
			}
		} else {
			try {
				confFile.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	static void load() {
		try {
			confusers.load(confFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (InvalidConfigurationException e) {
			e.printStackTrace();
		}
	}

	static void save() {
		try {
			confFile.createNewFile();
			confusers.save(confFile);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	static void clear() {
		ConfigurationSection userSection = confusers
				.getConfigurationSection("users");
		if (userSection == null)
			return;
		for (String user : userSection.getKeys(false)) {
			// clear cooldown
			ConfigurationSection cooldown = confusers
					.getConfigurationSection("users." + user + ".cooldown");
			if (cooldown != null) {
				for (String key : cooldown.getKeys(false)) {
					confusers.set("users." + user + ".cooldown." + key, null);
				}
			}
			confusers.set("users." + user + ".cooldown", null);

			// clear warmup
			ConfigurationSection warmup = confusers
					.getConfigurationSection("users." + user + ".warmup");
			if (warmup != null) {
				for (String key : warmup.getKeys(false)) {
					confusers.set("users." + user + ".warmup." + key, null);
				}
			}
			confusers.set("users." + user + ".warmup", null);

			confusers.set("users." + user, null);
		}
		save();
		load();
	}

	static void getLimits(Player player) {
		int usesNum = 0;
		int limitNum = 0;
		int num;
		String message;
		String lim = boosConfigManager.getLimGrp(player);
		ConfigurationSection uses = boosConfigManager.getLimits(player);
		if (uses != null) {
			for (String key : uses.getKeys(false)) {
				usesNum = confusers.getInt("users." + player.getName().toLowerCase() + ".uses." + key, usesNum);
				if (lim.equals("limit")) {
					limitNum = boosConfigManager.getLimit(key);
				} else if (lim.equals("limit2")) {
					limitNum = boosConfigManager.getLimit2(key);
				} else if (lim.equals("limit3")) {
					limitNum = boosConfigManager.getLimit3(key);
				} else if (lim.equals("limit4")) {
					limitNum = boosConfigManager.getLimit4(key);
				} else if (lim.equals("limit5")) {
					limitNum = boosConfigManager.getLimit5(key);
				}
				num = limitNum - usesNum;
				if (num < 0){
					num = 0;
				}
			message = boosConfigManager.getLimitListMessage();
			message = message.replaceAll("&command&", key);
			message = message.replaceAll("&limit&", String.valueOf(limitNum));
			message = message.replaceAll("&times&", String.valueOf(num));
			boosChat.sendMessageToPlayer(player, message);
			}
		}
	}

	static void clearSomething(String co, String player) {
		ConfigurationSection userSection = confusers
				.getConfigurationSection("users." + player + "." + co);
		if (userSection == null)
			return;
		confusers.set("users." + player + "." + co, null);
		save();
		load();
	}

	static void clearSomething(String co, String player, String command) {
		// ConfigurationSection userSection = confusers
		// .getConfigurationSection("users."+player+"."+co+"."+command);
		// if (userSection == null)
		// return;
		confusers.set("users." + player + "." + co + "." + command, 0);
		save();
		load();
	}

	static boolean coolDown(Player player, String pre) {
		pre = pre.toLowerCase();
		int coolDownSeconds = 0;
		coolDownSeconds = getCooldownGroup(player, pre, coolDownSeconds);
			if (coolDownSeconds > 0
					&& !player.hasPermission("booscooldowns.nocooldown")
					&& !player.hasPermission("booscooldowns.nocooldown." + pre)) {
				return cd(player, pre, coolDownSeconds);
			}
		return false;
	}

	private static boolean cd(Player player, String pre, int coolDownSeconds) {
		Date lastTime = getTime(player, pre);
		String link = boosConfigManager.getLink(pre);
		if (lastTime == null) {
			if (link == null) {
				setTime(player, pre);
			} else {
				List<String> linkGroup = boosConfigManager.getLinkList(link);
				for (String a : linkGroup) {
					setTime(player, a);
				}
			}
			return false;
		} else {
			Calendar calcurrTime = Calendar.getInstance();
			calcurrTime.setTime(getCurrTime());
			Calendar callastTime = Calendar.getInstance();
			callastTime.setTime(lastTime);
			long secondsBetween = secondsBetween(callastTime, calcurrTime);
			long waitSeconds = coolDownSeconds - secondsBetween;
			long waitMinutes = Math.round(waitSeconds / 60) + 1;
			long waitHours = Math.round(waitMinutes / 60) + 1;
			if (secondsBetween > coolDownSeconds) {
				if (link == null) {
					setTime(player, pre);
				} else {
					List<String> linkGroup = boosConfigManager
							.getLinkList(link);
					for (String a : linkGroup) {
						setTime(player, a);
					}
				}
				return false;
			} else {
				String msg = boosConfigManager.getCoolDownMessage();
				msg = msg.replaceAll("&command&", pre);
				if (waitSeconds >= 60 && 3600 >= waitSeconds) {
					msg = msg.replaceAll("&seconds&",
							Long.toString(waitMinutes));
					msg = msg.replaceAll("&unit&",
							boosConfigManager.getUnitMinutesMessage());
				} else if (waitMinutes >= 60) {
					msg = msg.replaceAll("&seconds&", Long.toString(waitHours));
					msg = msg.replaceAll("&unit&",
							boosConfigManager.getUnitHoursMessage());
				} else {
					String secs = Long.toString(waitSeconds);
					if (secs.equals("0")){
						secs = "1";
					}
					msg = msg.replaceAll("&seconds&",
							secs);
					msg = msg.replaceAll("&unit&",
							boosConfigManager.getUnitSecondsMessage());
				}
				boosChat.sendMessageToPlayer(player, msg);
				return true;
			}
		}
	}

	static void cancelCooldown(Player player, String pre) {
		confusers.set("users." + player.getName().toLowerCase() + ".cooldown." + pre, null);
	}

	static boolean checkCoolDownOK(Player player, String pre, String message) {
		pre = pre.toLowerCase();
		int coolDownSeconds = 0;
		coolDownSeconds = getCooldownGroup(player, pre, coolDownSeconds);
		if (coolDownSeconds > 0) {
			Date lastTime = getTime(player, pre);
			if (lastTime == null) {
				return true;
			} else {
				Calendar calcurrTime = Calendar.getInstance();
				calcurrTime.setTime(getCurrTime());
				Calendar callastTime = Calendar.getInstance();
				callastTime.setTime(lastTime);
				long secondsBetween = secondsBetween(callastTime, calcurrTime);
				long waitSeconds = coolDownSeconds - secondsBetween;
				long waitMinutes = Math.round(waitSeconds / 60) + 1;
				long waitHours = Math.round(waitMinutes / 60) + 1;
				if (secondsBetween > coolDownSeconds) {
					return true;
				} else {
					String msg = boosConfigManager.getCoolDownMessage();
					msg = msg.replaceAll("&command&", pre);
					if (waitSeconds >= 60 && 3600 >= waitSeconds) {
						msg = msg.replaceAll("&seconds&",
								Long.toString(waitMinutes));
						msg = msg.replaceAll("&unit&",
								boosConfigManager.getUnitMinutesMessage());
					} else if (waitMinutes >= 60) {
						msg = msg.replaceAll("&seconds&",
								Long.toString(waitHours));
						msg = msg.replaceAll("&unit&",
								boosConfigManager.getUnitHoursMessage());
					} else {
						msg = msg.replaceAll("&seconds&",
								Long.toString(waitSeconds));
						msg = msg.replaceAll("&unit&",
								boosConfigManager.getUnitSecondsMessage());
					}
					boosChat.sendMessageToPlayer(player, msg);
					return false;
				}
			}
		}
		return true;
	}

	private static int getCooldownGroup(Player player, String pre,
			int coolDownSeconds) {
			if (player.hasPermission("booscooldowns.cooldown2")) {
				coolDownSeconds = boosConfigManager.getCoolDown2(pre);
			} else if (player.hasPermission("booscooldowns.cooldown3")) {
				coolDownSeconds = boosConfigManager.getCoolDown3(pre);
			} else if (player.hasPermission("booscooldowns.cooldown4")) {
				coolDownSeconds = boosConfigManager.getCoolDown4(pre);
			} else if (player.hasPermission("booscooldowns.cooldown5")) {
				coolDownSeconds = boosConfigManager.getCoolDown5(pre);
			} else {
				coolDownSeconds = boosConfigManager.getCoolDown(pre);
			}
		return coolDownSeconds;
	}

	static void setTime(Player player, String pre) {
		pre = pre.toLowerCase();
		String currTime = "";
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		currTime = sdf.format(cal.getTime());
		confusers.set("users." + player.getName().toLowerCase() + ".cooldown." + pre,
				currTime);
	}

	static Date getCurrTime() {
		String currTime = "";
		Calendar cal = Calendar.getInstance();
		SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
		currTime = sdf.format(cal.getTime());
		Date time = null;

		try {
			time = sdf.parse(currTime);
			return time;
		} catch (ParseException e) {
			return null;
		}
	}

	static Date getTime(Player player, String pre) {
		pre = pre.toLowerCase();
		String confTime = "";
		confTime = confusers.getString("users." + player.getName().toLowerCase()
				+ ".cooldown." + pre, null);

		if (confTime != null && !confTime.equals("")) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm:ss");
			Date lastDate = null;

			try {
				lastDate = sdf.parse(confTime);
				return lastDate;
			} catch (ParseException e) {
				return null;
			}
		}
		return null;
	}

	public static long secondsBetween(Calendar startDate, Calendar endDate) {
		long secondsBetween = 0;

		while (startDate.before(endDate)) {
			startDate.add(Calendar.SECOND, 1);
			secondsBetween++;
		}
		return secondsBetween;
	}

	static void setWarmUpOK(Player player, String pre, String message) {
		pre = pre.toLowerCase();
		confusers.set("users." + player.getName().toLowerCase() + ".warmup." + pre, 1);
	}

	static boolean checkWarmUpOK(Player player, String pre, String message) {
		pre = pre.toLowerCase();
		int ok = 0;
		ok = confusers.getInt("users." + player.getName().toLowerCase() + ".warmup." + pre,
				ok);
		if (ok == 1) {
			return true;
		}
		return false;
	}

	static void removeWarmUpOK(Player player, String pre, String message) {
		pre = pre.toLowerCase();
		confusers.set("users." + player.getName().toLowerCase() + ".warmup." + pre, null);
	}

	static void removeWarmUp(Player player, String pre, String message) {
		pre = pre.toLowerCase();
		confusers.set("users." + player.getName().toLowerCase() + ".warmup." + pre, null);
	}

	static void setUses(Player player, String pre, String message) {
		pre = pre.toLowerCase();
		int uses = getUses(player, pre, message);
		uses = uses + 1;
		try {
			confusers.set("users." + player.getName().toLowerCase() + ".uses." + pre
					+ message, uses);
		} catch (IllegalArgumentException e) {
			boosCoolDown.log.warning("Player " + player.getName()
					+ " used empty command and caused this error!");
		}
	}

	static int getUses(Player player, String pre, String message) {
		pre = pre.toLowerCase();
		int uses = 0;
		uses = confusers.getInt("users." + player.getName().toLowerCase() + ".uses." + pre
				+ message, uses);
		return uses;
	}

}
