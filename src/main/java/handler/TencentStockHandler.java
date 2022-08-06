package handler;

import bean.StockBean;
import org.apache.commons.lang.StringUtils;
import utils.HttpClientPool;
import utils.LogUtil;

import javax.swing.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class TencentStockHandler extends StockRefreshHandler {
    private String urlPara;
    private HashMap<String, String[]> codeMap;
    private JLabel refreshTimeLabel;


    public TencentStockHandler(JTable table1, JLabel refreshTimeLabel) {
        super(table1);
        this.refreshTimeLabel = refreshTimeLabel;
    }

    @Override
    public void handle(List<String> code) {

        //LogUtil.info("Leeks 更新Stock编码数据.");
//        clearRow();
        if (code.isEmpty()) {
            return;
        }

        //股票编码，英文分号分隔（成本价和成本接在编码后用逗号分隔）
        List<String> codeList = new ArrayList<>();
        codeMap = new HashMap<>();
        for (String str : code) {
            //兼容原有设置
            String[] strArray;
            if (str.contains(",")) {
                strArray = str.split(",");
            } else {
                strArray = new String[]{str};
            }
            codeList.add(strArray[0]);
            codeMap.put(strArray[0], strArray);
        }

        urlPara = String.join(",", codeList);
        stepAction();

    }

    @Override
    public void stopHandle() {
        LogUtil.info("Leeks 准备停止更新Stock编码数据.");
    }

    private void stepAction() {
        if (StringUtils.isEmpty(urlPara)) {
            return;
        }
        try {
            String result = HttpClientPool.getHttpClient().get("http://qt.gtimg.cn/q=" + urlPara);
            parse(result);
            updateUI();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

	HashMap<String, Double> codeRecord = new HashMap<>();
    private void parse(String result) {
	   // 0: 未知 1: 名字 2: 代码 3: 当前价格 4: 昨收 5: 今开 6: 成交量（手） 7: 外盘 8: 内盘 9: 买一 10: 买一量（手） 11-18: 买二 买五 19: 卖一
	    // 20: 卖一量 21-28: 卖二 卖五 29: 最近逐笔成交 30: 时间 31: 涨跌 32: 涨跌% 33: 最高 34: 最低 35: 价格/成交量（手）/成交额 36: 成交量（手）
	    // 37: 成交额（万） 38: 换手率 39: 市盈率 40: 41: 最高 42: 最低 43: 振幅 44: 流通市值 45: 总市值 46: 市净率 47: 涨停价 48: 跌停价
	    //Double精度问题不重要
	    String[] lines = result.split("\n");
        for (String line : lines) {
            String code = line.substring(line.indexOf("_") + 1, line.indexOf("="));
            String dataStr = line.substring(line.indexOf("=") + 2, line.length() - 2);
            String[] values = dataStr.split("~");
            StockBean bean = new StockBean(code, codeMap);
            bean.setName(values[1]);
	        bean.setStartPrice(values[5]);
            bean.setNow(values[3]);
            bean.setChange(values[31]);
	        String value = values[32];
	        bean.setChangePercent(value);
	        Double oldChange = codeRecord.get(code);
	        double nowChange = Double.parseDouble(value);
	        if (oldChange != null) {
		        Double num = nowChange - oldChange;
		        if (Math.abs(num) >= 0.5) {
			        LogUtil.notify(code + " 收益变动0.5", true);
			        codeRecord.put(code, nowChange);
		        }
	        } else if (Math.abs(nowChange) >= 1) {
		        codeRecord.put(code, 1D);
	        }
            bean.setTime(values[30]);
            bean.setMax(values[33]);//33
            bean.setMin(values[34]);//34

            BigDecimal now = new BigDecimal(values[3]);
            String costPriceStr = bean.getCostPrise();
            if (StringUtils.isNotEmpty(costPriceStr)) {
                BigDecimal costPriceDec = new BigDecimal(costPriceStr);
                BigDecimal incomeDiff = now.add(costPriceDec.negate());
                BigDecimal incomePercentDec = incomeDiff.divide(costPriceDec, 5, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.TEN)
                        .multiply(BigDecimal.TEN)
                        .setScale(3, RoundingMode.HALF_UP);
                bean.setIncomePercent(incomePercentDec.toString());

                String bondStr = bean.getBonds();
                if (StringUtils.isNotEmpty(bondStr)) {
                    BigDecimal bondDec = new BigDecimal(bondStr);
                    BigDecimal incomeDec = incomeDiff.multiply(bondDec)
                            .setScale(2, RoundingMode.HALF_UP);
                    bean.setIncome(incomeDec.toString());
                }
            }

            updateData(bean);
        }
    }

    public void updateUI() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                refreshTimeLabel.setText(LocalDateTime.now().format(TianTianFundHandler.timeFormatter));
                refreshTimeLabel.setToolTipText("最后刷新时间");
            }
        });
    }


}
