package changcun.desktop_utils.ui;

import changcun.desktop_utils.model.ShutdownConfig;
import changcun.desktop_utils.service.HolidayStore;
import changcun.desktop_utils.service.ShutdownScheduler;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.SpinnerDateModel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;

/**
 * 第二个页面：设置定时关机（一次性 / 每天 / 非节假日），可持久化并可随时修改。
 */
public class ShutdownPanel extends JPanel {

    private static final DateTimeFormatter STATUS_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ShutdownScheduler scheduler;
    private final HolidayStore holidayStore;

    private final JRadioButton noneRadio = new JRadioButton("关闭定时关机");
    private final JRadioButton onceRadio = new JRadioButton("一次性关机");
    private final JRadioButton dailyRadio = new JRadioButton("每天关机");
    private final JRadioButton workdayRadio = new JRadioButton("非节假日关机");

    private final JSpinner dateTimeSpinner;
    private final JSpinner timeSpinner;
    private final JSpinner workdayTimeSpinner;
    private final JLabel statusLabel = new JLabel();

    public ShutdownPanel(ShutdownScheduler scheduler, HolidayStore holidayStore) {
        this.scheduler = scheduler;
        this.holidayStore = holidayStore;

        setLayout(new BorderLayout(0, 16));
        setBackground(UiTheme.WINDOW_BG);
        setBorder(new EmptyBorder(20, 20, 20, 20));

        // 一次性关机：日期 + 时间
        SpinnerDateModel dateModel = new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE);
        dateTimeSpinner = new JSpinner(dateModel);
        dateTimeSpinner.setEditor(new JSpinner.DateEditor(dateTimeSpinner, "yyyy-MM-dd HH:mm"));

        // 每天关机：时间
        SpinnerDateModel timeModel = new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE);
        timeSpinner = new JSpinner(timeModel);
        timeSpinner.setEditor(new JSpinner.DateEditor(timeSpinner, "HH:mm"));

        // 非节假日关机：时间
        SpinnerDateModel workdayModel = new SpinnerDateModel(new Date(), null, null, Calendar.MINUTE);
        workdayTimeSpinner = new JSpinner(workdayModel);
        workdayTimeSpinner.setEditor(new JSpinner.DateEditor(workdayTimeSpinner, "HH:mm"));

        add(buildHeader(), BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setOpaque(false);
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.add(buildStatusCard());
        content.add(Box.createVerticalStrut(16));
        content.add(buildSettingsCard());
        content.add(Box.createVerticalStrut(16));
        content.add(buildButtonRow());
        add(content, BorderLayout.CENTER);

        // 单选互斥
        ButtonGroup group = new ButtonGroup();
        group.add(noneRadio);
        group.add(onceRadio);
        group.add(dailyRadio);
        group.add(workdayRadio);

        // 事件绑定
        noneRadio.addActionListener(e -> updateSpinnerState());
        onceRadio.addActionListener(e -> updateSpinnerState());
        dailyRadio.addActionListener(e -> updateSpinnerState());
        workdayRadio.addActionListener(e -> updateSpinnerState());

        // 从持久化配置初始化界面
        syncFromConfig();

        // 每秒刷新一次状态提示（剩余时间等）
        Timer timer = new Timer(1000, e -> updateStatus());
        timer.start();
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel();
        header.setOpaque(false);
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.add(UiTheme.title("定时关机"));
        JLabel subtitle = UiTheme.subtitle("到达设定时间后自动关闭计算机，可随时修改或取消");
        subtitle.setBorder(new EmptyBorder(4, 0, 0, 0));
        header.add(subtitle);
        return header;
    }

    private JPanel buildStatusCard() {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(UiTheme.ACCENT_SOFT);
        card.setBorder(new EmptyBorder(14, 18, 14, 18));
        statusLabel.setForeground(UiTheme.TEXT_PRIMARY);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.PLAIN, 14f));
        card.add(statusLabel, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildSettingsCard() {
        JPanel card = UiTheme.card();
        card.setLayout(new BorderLayout(0, 14));

        JLabel section = UiTheme.sectionTitle("关机设置");
        card.add(section, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(8, 8, 8, 8);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        gc.gridx = 0;
        gc.gridy = 0;
        gc.gridwidth = 2;
        gc.weightx = 1.0;
        form.add(noneRadio, gc);

        gc.gridwidth = 1;
        gc.weightx = 0.0;
        gc.gridy = 1;
        gc.gridx = 0;
        form.add(onceRadio, gc);
        gc.gridx = 1;
        gc.weightx = 1.0;
        form.add(dateTimeSpinner, gc);

        gc.weightx = 0.0;
        gc.gridy = 2;
        gc.gridx = 0;
        form.add(dailyRadio, gc);
        gc.gridx = 1;
        gc.weightx = 1.0;
        form.add(timeSpinner, gc);

        gc.weightx = 0.0;
        gc.gridy = 3;
        gc.gridx = 0;
        form.add(workdayRadio, gc);
        gc.gridx = 1;
        gc.weightx = 1.0;
        form.add(workdayTimeSpinner, gc);

        card.add(form, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildButtonRow() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        row.setOpaque(false);

        JButton applyButton = UiTheme.primaryButton("应用设置");
        applyButton.addActionListener(e -> apply());

        JButton cancelButton = UiTheme.secondaryButton("取消定时关机");
        cancelButton.addActionListener(e -> cancel());

        row.add(applyButton);
        row.add(cancelButton);
        return row;
    }

    private void updateSpinnerState() {
        dateTimeSpinner.setEnabled(onceRadio.isSelected());
        timeSpinner.setEnabled(dailyRadio.isSelected());
        workdayTimeSpinner.setEnabled(workdayRadio.isSelected());
    }

    private void syncFromConfig() {
        ShutdownConfig c = scheduler.getConfig();
        switch (c.getMode()) {
            case ONCE:
                onceRadio.setSelected(true);
                if (c.getOnceEpochMillis() > 0) {
                    dateTimeSpinner.setValue(new Date(c.getOnceEpochMillis()));
                }
                break;
            case DAILY:
                dailyRadio.setSelected(true);
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.HOUR_OF_DAY, c.getDailyHour());
                cal.set(Calendar.MINUTE, c.getDailyMinute());
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                timeSpinner.setValue(cal.getTime());
                break;
            case WORKDAY:
                workdayRadio.setSelected(true);
                Calendar wcal = Calendar.getInstance();
                wcal.set(Calendar.HOUR_OF_DAY, c.getWorkdayHour());
                wcal.set(Calendar.MINUTE, c.getWorkdayMinute());
                wcal.set(Calendar.SECOND, 0);
                wcal.set(Calendar.MILLISECOND, 0);
                workdayTimeSpinner.setValue(wcal.getTime());
                break;
            case NONE:
            default:
                noneRadio.setSelected(true);
                break;
        }
        updateSpinnerState();
        updateStatus();
    }

    private void apply() {
        ShutdownConfig c = new ShutdownConfig();
        if (onceRadio.isSelected()) {
            long millis = ((Date) dateTimeSpinner.getValue()).getTime();
            if (millis <= System.currentTimeMillis()) {
                JOptionPane.showMessageDialog(this,
                        "关机时间必须晚于当前时间。",
                        "提示",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            c.setMode(ShutdownConfig.Mode.ONCE);
            c.setOnceEpochMillis(millis);
        } else if (dailyRadio.isSelected()) {
            Date d = (Date) timeSpinner.getValue();
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            c.setMode(ShutdownConfig.Mode.DAILY);
            c.setDailyHour(cal.get(Calendar.HOUR_OF_DAY));
            c.setDailyMinute(cal.get(Calendar.MINUTE));
        } else if (workdayRadio.isSelected()) {
            if (holidayStore.load().size() == 0) {
                int choice = JOptionPane.showConfirmDialog(this,
                        "当前还没有导入节假日信息，非节假日模式将把每天都视为工作日。\n是否仍然启用？",
                        "提示",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (choice != JOptionPane.YES_OPTION) {
                    return;
                }
            }
            Date d = (Date) workdayTimeSpinner.getValue();
            Calendar cal = Calendar.getInstance();
            cal.setTime(d);
            c.setMode(ShutdownConfig.Mode.WORKDAY);
            c.setWorkdayHour(cal.get(Calendar.HOUR_OF_DAY));
            c.setWorkdayMinute(cal.get(Calendar.MINUTE));
        } else {
            c.setMode(ShutdownConfig.Mode.NONE);
        }

        scheduler.updateConfig(c);
        updateStatus();
    }

    private void cancel() {
        scheduler.cancel();
        noneRadio.setSelected(true);
        updateSpinnerState();
        updateStatus();
    }

    private void updateStatus() {
        ShutdownConfig c = scheduler.getConfig();
        long now = System.currentTimeMillis();
        long next = scheduler.nextTriggerMillis(now);

        if (c.getMode() == ShutdownConfig.Mode.NONE || next <= 0) {
            statusLabel.setText("当前状态：未设置定时关机");
            return;
        }

        String when = Instant.ofEpochMilli(next)
                .atZone(ZoneId.systemDefault())
                .format(STATUS_TIME);
        long remain = Math.max(0, next - now);
        String remainText = formatDuration(remain);

        if (c.getMode() == ShutdownConfig.Mode.DAILY) {
            statusLabel.setText(String.format(
                    "<html>当前状态：每天 %02d:%02d 自动关机<br>下一次：%s（剩余 %s）</html>",
                    c.getDailyHour(), c.getDailyMinute(), when, remainText));
        } else if (c.getMode() == ShutdownConfig.Mode.WORKDAY) {
            statusLabel.setText(String.format(
                    "<html>当前状态：非节假日每天 %02d:%02d 自动关机<br>下一次：%s（剩余 %s）</html>",
                    c.getWorkdayHour(), c.getWorkdayMinute(), when, remainText));
        } else {
            statusLabel.setText(String.format(
                    "<html>当前状态：将在 %s 自动关机（剩余 %s）</html>",
                    when, remainText));
        }
    }

    private static String formatDuration(long millis) {
        long totalSeconds = millis / 1000;
        long days = totalSeconds / 86400;
        long hours = (totalSeconds % 86400) / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (days > 0) {
            return String.format("%d 天 %d 小时 %d 分钟", days, hours, minutes);
        }
        if (hours > 0) {
            return String.format("%d 小时 %d 分钟 %d 秒", hours, minutes, seconds);
        }
        if (minutes > 0) {
            return String.format("%d 分钟 %d 秒", minutes, seconds);
        }
        return String.format("%d 秒", seconds);
    }
}
