(function () {
    "use strict";

    const form = document.getElementById("backtest-form");
    const runButton = document.getElementById("run-button");
    const exampleButton = document.getElementById("example-button");
    const statusDot = document.getElementById("status-dot");
    const statusText = document.getElementById("status-text");
    const statusMeta = document.getElementById("status-meta");

    const charts = {
        price: { chart: null, observer: null },
        equity: { chart: null, observer: null }
    };

    const numberFormat = new Intl.NumberFormat("ko-KR", {
        maximumFractionDigits: 6
    });
    const moneyFormat = new Intl.NumberFormat("ko-KR", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    const percentFormat = new Intl.NumberFormat("ko-KR", {
        minimumFractionDigits: 2,
        maximumFractionDigits: 2
    });
    const dateTimeFormat = new Intl.DateTimeFormat("ko-KR", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit"
    });

    const eventMeta = {
        ENTRY: { label: "ENTRY", color: "#138a62", shape: "arrowUp" },
        TAKE_PROFIT: { label: "TP", color: "#2563eb", shape: "circle" },
        STOP_LOSS: { label: "SL", color: "#c4473d", shape: "circle" },
        SUPER_TREND_EXIT: { label: "ST", color: "#7c3aed", shape: "circle" },
        LIQUIDATION: { label: "LIQUIDATION", color: "#991b1b", shape: "arrowDown" },
        END_OF_DATA_EXIT: { label: "END_OF_DATA", color: "#6b7280", shape: "square" },
        STOP_PROMOTION: { label: "승격", color: "#c98213", shape: "circle" },
        STOP_PROMOTION_DEFERRED: { label: "승격", color: "#9a6a16", shape: "circle" }
    };

    const exitReasonMeta = {
        TAKE_PROFIT: { label: "TP", color: "#2563eb", shape: "circle" },
        STOP_LOSS: { label: "SL", color: "#c4473d", shape: "circle" },
        SUPER_TREND: { label: "ST", color: "#7c3aed", shape: "circle" },
        LIQUIDATION: { label: "LIQUIDATION", color: "#991b1b", shape: "arrowDown" },
        END_OF_DATA: { label: "END_OF_DATA", color: "#6b7280", shape: "square" }
    };

    document.addEventListener("DOMContentLoaded", initialize);

    function initialize() {
        form.addEventListener("submit", handleSubmit);
        exampleButton.addEventListener("click", resetToExample);
        clearOutput();
    }

    async function handleSubmit(event) {
        event.preventDefault();
        hideError();

        let requestBody;
        try {
            requestBody = buildRequestBody();
        } catch (error) {
            setStatus("error", "오류", error.message);
            showError({ message: error.message });
            return;
        }

        const startedAt = performance.now();
        setLoading(true);
        setStatus("loading", "실행 중", `${requestBody.symbol} ${requestBody.interval}`);

        try {
            const response = await fetch("/api/backtests", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Accept": "application/json"
                },
                body: JSON.stringify(requestBody)
            });

            const payload = await parseResponse(response);
            if (!response.ok) {
                throw toApiError(response, payload);
            }

            renderBacktestResponse(payload);
            const elapsedSec = ((performance.now() - startedAt) / 1000).toFixed(2);
            const candleCount = Array.isArray(payload.chartPoints) ? payload.chartPoints.length : 0;
            setStatus("success", "완료", `${payload.symbol} · 캔들 ${candleCount}개 · ${elapsedSec}s`);
        } catch (error) {
            setStatus("error", "오류", error.message || "요청 처리 중 오류가 발생했습니다.");
            showError(error);
        } finally {
            setLoading(false);
        }
    }

    function buildRequestBody() {
        const startTimeMs = readDateTimeMs("startTime");
        const endTimeMs = readDateTimeMs("endTime");

        if (endTimeMs <= startTimeMs) {
            throw new Error("종료 시간은 시작 시간보다 뒤여야 합니다.");
        }

        const takeProfitLevels = [
            takeProfitLevel("tpTarget1", "tpRatio1"),
            takeProfitLevel("tpTarget2", "tpRatio2"),
            takeProfitLevel("tpTarget3", "tpRatio3")
        ];

        const closeRatioSum = takeProfitLevels.reduce((sum, level) => sum + level.closeRatio, 0);
        if (Math.abs(closeRatioSum - 1) > 1e-9) {
            throw new Error("분할 청산 비율의 합은 1.0이어야 합니다.");
        }

        return {
            symbol: readText("symbol"),
            interval: readText("interval"),
            startTimeMs,
            endTimeMs,
            initialBalance: readNumber("initialBalance"),
            feeRate: readNumber("feeRate"),
            slippageRate: readNumber("slippageRate"),
            leverage: readInteger("leverage"),
            marginUsageRate: readNumber("marginUsageRate"),
            bollingerPeriod: readInteger("bollingerPeriod"),
            bollingerMultiplier: readNumber("bollingerMultiplier"),
            bollingerWidthMinPercent: readNumber("bollingerWidthMinPercent"),
            superTrendLength: readInteger("superTrendLength"),
            superTrendFactor: readNumber("superTrendFactor"),
            entryMode: readText("entryMode"),
            maTolerancePercent: readNumber("maTolerancePercent"),
            bbTolerancePercent: readNumber("bbTolerancePercent"),
            oneEntryPerTrend: readChecked("oneEntryPerTrend"),
            riskManagement: {
                stopLossPercent: readNumber("stopLossPercent"),
                takeProfitLevels,
                stopPromotionMode: readText("stopPromotionMode")
            },
            liquidation: {
                maintenanceMarginRate: readNumber("maintenanceMarginRate"),
                maintenanceAmount: readNumber("maintenanceAmount"),
                rejectEntryIfLiquidationBeforeStop: readChecked("rejectEntryIfLiquidationBeforeStop"),
                stopBufferPercent: readNumber("stopBufferPercent")
            }
        };
    }

    function takeProfitLevel(targetName, ratioName) {
        return {
            targetPercent: readNumber(targetName),
            closeRatio: readNumber(ratioName)
        };
    }

    function readText(name) {
        const input = form.elements[name];
        const value = input ? String(input.value).trim() : "";
        if (!value) {
            throw new Error(`${name} 값을 입력하세요.`);
        }
        return value;
    }

    function readNumber(name) {
        const input = form.elements[name];
        const value = input ? Number(input.value) : NaN;
        if (!Number.isFinite(value)) {
            throw new Error(`${name} 값이 올바른 숫자가 아닙니다.`);
        }
        return value;
    }

    function readInteger(name) {
        const value = readNumber(name);
        if (!Number.isInteger(value)) {
            throw new Error(`${name} 값은 정수여야 합니다.`);
        }
        return value;
    }

    function readChecked(name) {
        const input = form.elements[name];
        return Boolean(input && input.checked);
    }

    function readDateTimeMs(name) {
        const input = form.elements[name];
        const value = input ? input.value : "";
        const timeMs = new Date(value).getTime();
        if (!Number.isFinite(timeMs)) {
            throw new Error(`${name} 값이 올바른 날짜 시간이 아닙니다.`);
        }
        return timeMs;
    }

    async function parseResponse(response) {
        const text = await response.text();
        if (!text) {
            return null;
        }

        try {
            return JSON.parse(text);
        } catch (error) {
            return { message: text };
        }
    }

    function toApiError(response, payload) {
        const error = new Error(payload && payload.message ? payload.message : `HTTP ${response.status}`);
        error.payload = payload;
        error.status = response.status;
        return error;
    }

    function renderBacktestResponse(response) {
        const result = response && response.result ? response.result : {};
        const chartPoints = Array.isArray(response.chartPoints) ? response.chartPoints : [];
        const events = Array.isArray(result.events) ? result.events : [];
        const equityCurve = Array.isArray(result.equityCurve) ? result.equityCurve : [];
        const trades = Array.isArray(result.trades) ? result.trades : [];

        renderSummary(response, result);
        renderPriceChart(chartPoints, events, trades);
        renderEquityChart(equityCurve);
        renderTrades(trades);
    }

    function renderSummary(response, result) {
        setMetric("metric-initial", formatMoney(result.initialBalance));
        setMetric("metric-final", formatMoney(result.finalBalance));
        setMetric("metric-return", formatPercent(result.totalReturnPercent), result.totalReturnPercent);
        setMetric("metric-trades", formatInteger(result.totalTrades));
        setMetric("metric-win-rate", formatPercent(result.winRatePercent));
        setMetric("metric-pnl", formatMoney(result.totalNetPnl), result.totalNetPnl);
        setMetric("metric-fee", formatMoney(result.totalFee));
        setMetric("metric-mdd", formatPercent(result.maxDrawdownPercent), -Math.abs(Number(result.maxDrawdownPercent || 0)));

        const range = document.getElementById("summary-range");
        if (response && Number.isFinite(Number(response.startTimeMs)) && Number.isFinite(Number(response.endTimeMs))) {
            range.textContent = `${formatDateTime(response.startTimeMs)} - ${formatDateTime(response.endTimeMs)}`;
        } else {
            range.textContent = "-";
        }
    }

    function setMetric(id, text, signedValue) {
        const element = document.getElementById(id);
        element.textContent = text;
        element.classList.remove("positive", "negative");

        if (Number.isFinite(Number(signedValue))) {
            const value = Number(signedValue);
            if (value > 0) {
                element.classList.add("positive");
            } else if (value < 0) {
                element.classList.add("negative");
            }
        }
    }

    function renderPriceChart(chartPoints, events, trades) {
        const container = document.getElementById("price-chart");
        disposeChart("price");

        if (!Array.isArray(chartPoints) || chartPoints.length === 0) {
            showChartPlaceholder(container, "캔들 데이터가 없습니다.");
            return;
        }

        if (!window.LightweightCharts) {
            showChartPlaceholder(container, "차트 라이브러리를 불러오지 못했습니다.");
            return;
        }

        container.replaceChildren();
        const chart = createChart(container);
        charts.price.chart = chart;
        charts.price.observer = observeChartResize(container, chart);

        const candleSeries = chart.addCandlestickSeries({
            upColor: "#138a62",
            downColor: "#c4473d",
            borderUpColor: "#138a62",
            borderDownColor: "#c4473d",
            wickUpColor: "#138a62",
            wickDownColor: "#c4473d"
        });

        const candleData = uniqueByTime(toCandles(chartPoints));
        candleSeries.setData(candleData);
        candleSeries.setMarkers(toPriceMarkers(events, trades));
        attachCandleTooltip(container, chart, candleSeries, chartPoints);

        addLineSeries(chart, chartPoints, "bollingerUpper", "#2563eb", 1);
        addLineSeries(chart, chartPoints, "bollingerBasis", "#6b7280", 1);
        addLineSeries(chart, chartPoints, "bollingerLower", "#c98213", 1);
        addSuperTrendSeries(chart, chartPoints);

        chart.timeScale().fitContent();
    }

    function attachCandleTooltip(container, chart, candleSeries, chartPoints) {
        const tooltip = document.createElement("div");
        tooltip.className = "chart-tooltip";
        tooltip.hidden = true;
        container.appendChild(tooltip);

        const sourceByTime = new Map(chartPoints
            .filter(point => isFiniteNumber(point.timeMs))
            .map(point => [toChartTime(point.timeMs), point]));

        chart.subscribeCrosshairMove(param => {
            if (
                !param ||
                !param.point ||
                param.point.x < 0 ||
                param.point.y < 0 ||
                param.point.x > container.clientWidth ||
                param.point.y > container.clientHeight
            ) {
                tooltip.hidden = true;
                return;
            }

            const candle = param.seriesData ? param.seriesData.get(candleSeries) : null;
            if (!candle || !isFiniteNumber(candle.open)) {
                tooltip.hidden = true;
                return;
            }

            const time = Number(param.time);
            const source = sourceByTime.get(time) || {};
            renderCandleTooltip(tooltip, source, candle, time);
            tooltip.hidden = false;

            const left = Math.min(param.point.x + 14, container.clientWidth - tooltip.offsetWidth - 8);
            const top = Math.min(param.point.y + 14, container.clientHeight - tooltip.offsetHeight - 8);
            tooltip.style.left = `${Math.max(8, left)}px`;
            tooltip.style.top = `${Math.max(8, top)}px`;
        });
    }

    function renderCandleTooltip(tooltip, source, candle, chartTime) {
        const open = Number(candle.open);
        const high = Number(candle.high);
        const low = Number(candle.low);
        const close = Number(candle.close);
        const changePercent = open === 0 ? NaN : (close - open) / open * 100;
        const rangePercent = open === 0 ? NaN : (high - low) / open * 100;
        const timeMs = isFiniteNumber(source.timeMs) ? Number(source.timeMs) : Number(chartTime) * 1000;

        const title = document.createElement("div");
        title.className = "tooltip-title";
        title.textContent = formatDateTime(timeMs);

        const grid = document.createElement("div");
        grid.className = "tooltip-grid";

        appendTooltipPair(grid, "시가", formatNumber(open));
        appendTooltipPair(grid, "고가", formatNumber(high));
        appendTooltipPair(grid, "저가", formatNumber(low));
        appendTooltipPair(grid, "종가", formatNumber(close));
        appendTooltipPair(grid, "등락률", formatSignedPercent(changePercent), percentClass(changePercent));
        appendTooltipPair(grid, "변동폭", formatPercent(rangePercent));

        tooltip.replaceChildren(title, grid);
    }

    function appendTooltipPair(grid, label, value, valueClass) {
        const labelElement = document.createElement("span");
        const valueElement = document.createElement("span");

        labelElement.textContent = label;
        valueElement.textContent = value;
        if (valueClass) {
            valueElement.classList.add(valueClass);
        }

        grid.append(labelElement, valueElement);
    }

    function renderEquityChart(equityCurve) {
        const container = document.getElementById("equity-chart");
        const count = document.getElementById("equity-count");
        disposeChart("equity");

        const data = uniqueByTime(equityCurve
            .filter(point => isFiniteNumber(point.timeMs) && isFiniteNumber(point.balance))
            .map(point => ({
                time: toChartTime(point.timeMs),
                value: Number(point.balance)
            })));

        count.textContent = data.length ? `${data.length}개 포인트` : "-";

        if (data.length === 0) {
            showChartPlaceholder(container, "자산 곡선 데이터가 없습니다.");
            return;
        }

        if (!window.LightweightCharts) {
            showChartPlaceholder(container, "차트 라이브러리를 불러오지 못했습니다.");
            return;
        }

        container.replaceChildren();
        const chart = createChart(container);
        charts.equity.chart = chart;
        charts.equity.observer = observeChartResize(container, chart);

        const areaSeries = chart.addAreaSeries({
            lineColor: "#2563eb",
            topColor: "rgba(37, 99, 235, 0.28)",
            bottomColor: "rgba(37, 99, 235, 0.03)",
            lineWidth: 2,
            priceLineVisible: false
        });

        areaSeries.setData(data);
        attachEquityTooltip(container, chart, areaSeries, data);
        chart.timeScale().fitContent();
    }

    function attachEquityTooltip(container, chart, areaSeries, data) {
        const tooltip = document.createElement("div");
        tooltip.className = "chart-tooltip";
        tooltip.hidden = true;
        container.appendChild(tooltip);
        const dataByTime = new Map(data.map(point => [Number(point.time), point]));

        chart.subscribeCrosshairMove(param => {
            if (
                !param ||
                !param.point ||
                param.point.x < 0 ||
                param.point.y < 0 ||
                param.point.x > container.clientWidth ||
                param.point.y > container.clientHeight
            ) {
                tooltip.hidden = true;
                return;
            }

            const point = resolveEquityPoint(param, chart, areaSeries, data, dataByTime);
            if (!point || !isFiniteNumber(point.value)) {
                tooltip.hidden = true;
                return;
            }

            const value = Number(point.value);
            const title = document.createElement("div");
            title.className = "tooltip-title";
            title.textContent = formatDateTime(Number(point.time) * 1000);

            const grid = document.createElement("div");
            grid.className = "tooltip-grid";
            appendTooltipPair(grid, "자산", formatMoney(value));

            tooltip.replaceChildren(title, grid);
            tooltip.hidden = false;

            const left = Math.min(param.point.x + 14, container.clientWidth - tooltip.offsetWidth - 8);
            const pointY = areaSeries.priceToCoordinate(value);
            const anchorY = Number.isFinite(pointY) ? pointY : param.point.y;
            const top = Math.min(anchorY + 14, container.clientHeight - tooltip.offsetHeight - 8);
            tooltip.style.left = `${Math.max(8, left)}px`;
            tooltip.style.top = `${Math.max(8, top)}px`;
        });
    }

    function resolveEquityPoint(param, chart, areaSeries, data, dataByTime) {
        const time = normalizeChartTime(param.time);
        const seriesPoint = param.seriesData ? param.seriesData.get(areaSeries) : null;

        if (seriesPoint && isFiniteNumber(seriesPoint.value)) {
            const matchedPoint = time === null ? null : dataByTime.get(time);
            return {
                time: matchedPoint ? matchedPoint.time : nearestEquityTime(param, chart, data, time),
                value: Number(seriesPoint.value)
            };
        }

        if (time !== null && dataByTime.has(time)) {
            return dataByTime.get(time);
        }

        const nearestTime = nearestEquityTime(param, chart, data, time);
        return nearestTime === null ? null : findNearestPointByTime(data, nearestTime);
    }

    function nearestEquityTime(param, chart, data, time) {
        if (time !== null) {
            const nearest = findNearestPointByTime(data, time);
            return nearest ? Number(nearest.time) : null;
        }

        const coordinateTime = normalizeChartTime(chart.timeScale().coordinateToTime(param.point.x));
        if (coordinateTime === null) {
            return null;
        }

        const nearest = findNearestPointByTime(data, coordinateTime);
        return nearest ? Number(nearest.time) : null;
    }

    function findNearestPointByTime(data, time) {
        if (!Array.isArray(data) || data.length === 0 || !isFiniteNumber(time)) {
            return null;
        }

        let nearest = data[0];
        let nearestDistance = Math.abs(Number(nearest.time) - Number(time));

        for (let i = 1; i < data.length; i++) {
            const distance = Math.abs(Number(data[i].time) - Number(time));
            if (distance < nearestDistance) {
                nearest = data[i];
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    function createChart(container) {
        const rootStyle = getComputedStyle(document.documentElement);
        const surfaceColor = rootStyle.getPropertyValue("--surface").trim() || "#ffffff";
        const textColor = rootStyle.getPropertyValue("--text").trim() || "#1f2937";
        const lineColor = rootStyle.getPropertyValue("--line").trim() || "#d9dee8";

        return LightweightCharts.createChart(container, {
            width: Math.max(container.clientWidth, 320),
            height: Math.max(container.clientHeight, 260),
            layout: {
                background: { type: "solid", color: surfaceColor },
                textColor
            },
            grid: {
                vertLines: { color: "#eef2f7" },
                horzLines: { color: "#eef2f7" }
            },
            rightPriceScale: {
                borderColor: lineColor
            },
            timeScale: {
                borderColor: lineColor,
                timeVisible: true,
                secondsVisible: false,
                minBarSpacing: 0.15,
                rightOffset: 4
            },
            handleScroll: {
                mouseWheel: true,
                pressedMouseMove: true,
                horzTouchDrag: true,
                vertTouchDrag: true
            },
            handleScale: {
                axisPressedMouseMove: true,
                mouseWheel: true,
                pinch: true
            },
            crosshair: {
                mode: LightweightCharts.CrosshairMode.Normal
            },
            localization: {
                priceFormatter: price => formatNumber(price)
            }
        });
    }

    function observeChartResize(container, chart) {
        const observer = new ResizeObserver(entries => {
            const entry = entries[0];
            if (!entry) {
                return;
            }

            chart.applyOptions({
                width: Math.max(Math.floor(entry.contentRect.width), 320),
                height: Math.max(Math.floor(container.clientHeight), 260)
            });
        });

        observer.observe(container);
        return observer;
    }

    function addLineSeries(chart, chartPoints, fieldName, color, width) {
        const series = chart.addLineSeries({
            color,
            lineWidth: width,
            lastValueVisible: false,
            priceLineVisible: false
        });

        series.setData(toLineData(chartPoints, point => point[fieldName]));
    }

    function addSuperTrendSeries(chart, chartPoints) {
        const segments = toSuperTrendSegments(chartPoints);

        segments.forEach(segment => {
            const series = chart.addLineSeries({
                color: segment.direction === "UP" ? "#138a62" : "#c4473d",
                lineWidth: 2,
                lastValueVisible: false,
                priceLineVisible: false
            });

            series.setData(segment.data);
        });
    }

    function toSuperTrendSegments(chartPoints) {
        const segments = [];
        let currentSegment = null;

        chartPoints
            .filter(point => isFiniteNumber(point.timeMs))
            .sort((a, b) => Number(a.timeMs) - Number(b.timeMs))
            .forEach(point => {
                const direction = point.superTrendDirection;
                const hasValue = (direction === "UP" || direction === "DOWN") && isFiniteNumber(point.superTrendValue);

                if (!hasValue) {
                    currentSegment = null;
                    return;
                }

                if (!currentSegment || currentSegment.direction !== direction) {
                    currentSegment = { direction, data: [] };
                    segments.push(currentSegment);
                }

                currentSegment.data.push({
                    time: toChartTime(point.timeMs),
                    value: Number(point.superTrendValue)
                });
            });

        return segments.filter(segment => segment.data.length > 0);
    }

    function toCandles(chartPoints) {
        return chartPoints
            .filter(point =>
                isFiniteNumber(point.timeMs) &&
                isFiniteNumber(point.open) &&
                isFiniteNumber(point.high) &&
                isFiniteNumber(point.low) &&
                isFiniteNumber(point.close)
            )
            .map(point => ({
                time: toChartTime(point.timeMs),
                open: Number(point.open),
                high: Number(point.high),
                low: Number(point.low),
                close: Number(point.close)
            }))
            .sort(sortByTime);
    }

    function toLineData(chartPoints, valueSelector) {
        return uniqueByTime(chartPoints
            .filter(point => isFiniteNumber(point.timeMs))
            .map(point => {
                const value = valueSelector(point);
                const time = toChartTime(point.timeMs);
                return isFiniteNumber(value) ? { time, value: Number(value) } : { time };
            })
            .sort(sortByTime));
    }

    function toMarkers(events) {
        if (!Array.isArray(events)) {
            return [];
        }

        const markers = [];

        events
            .filter(event => event && eventMeta[event.type] && isFiniteNumber(event.timeMs))
            .forEach(event => {
                const meta = eventMeta[event.type];
                markers.push({
                    time: toChartTime(event.timeMs),
                    position: markerPosition(event),
                    color: markerColor(event, meta),
                    shape: markerShape(event, meta),
                    text: markerText(event, meta)
                });
            });

        return markers.sort(sortByTime);
    }

    function toPriceMarkers(events, trades) {
        const tradeMarkers = toTradeMarkers(trades);

        if (tradeMarkers.length === 0) {
            return toMarkers(events);
        }

        const runtimeMarkers = Array.isArray(events)
            ? toMarkers(events.filter(isRuntimeMarkerEvent))
            : [];

        return tradeMarkers.concat(runtimeMarkers).sort(sortByTime);
    }

    function toTradeMarkers(trades) {
        if (!Array.isArray(trades)) {
            return [];
        }

        const markers = [];

        trades.forEach(trade => {
            if (!trade || !isFiniteNumber(trade.entryTimeMs)) {
                return;
            }

            markers.push({
                time: toChartTime(trade.entryTimeMs),
                position: markerPosition({ type: "ENTRY", side: trade.side }),
                color: markerColor({ type: "ENTRY", side: trade.side }, eventMeta.ENTRY),
                shape: markerShape({ type: "ENTRY", side: trade.side }, eventMeta.ENTRY),
                text: markerText({
                    type: "ENTRY",
                    side: trade.side
                }, eventMeta.ENTRY)
            });

            let takeProfitIndex = 0;
            const exits = Array.isArray(trade.exits) ? trade.exits : [];

            exits.forEach(exit => {
                const reason = exit ? exit.exitReason : null;
                const meta = exitReasonMeta[reason];

                if (!meta || !isFiniteNumber(exit.exitTimeMs)) {
                    return;
                }

                if (reason === "TAKE_PROFIT") {
                    takeProfitIndex += 1;
                }

                markers.push({
                    time: toChartTime(exit.exitTimeMs),
                    position: markerPosition({ type: "TAKE_PROFIT", side: trade.side }),
                    color: meta.color,
                    shape: meta.shape,
                    text: exitMarkerText(exit, meta, takeProfitIndex)
                });
            });
        });

        return markers.sort(sortByTime);
    }

    function isRuntimeMarkerEvent(event) {
        return event &&
            (event.type === "STOP_PROMOTION" || event.type === "STOP_PROMOTION_DEFERRED");
    }

    function exitMarkerText(exit, meta, takeProfitIndex) {
        if (exit.exitReason === "TAKE_PROFIT") {
            return `${meta.label}${takeProfitIndex}`;
        }

        if (exit.exitReason === "STOP_LOSS") {
            return meta.label;
        }

        if (exit.exitReason === "SUPER_TREND") {
            return meta.label;
        }

        return "";
    }

    function markerPosition(event) {
        if (event.type === "ENTRY") {
            return event.side === "SHORT" ? "aboveBar" : "belowBar";
        }
        return event.side === "SHORT" ? "belowBar" : "aboveBar";
    }

    function markerColor(event, meta) {
        if (event.type === "ENTRY") {
            return event.side === "SHORT" ? "#c4473d" : "#138a62";
        }
        return meta.color;
    }

    function markerShape(event, meta) {
        if (event.type === "ENTRY") {
            return event.side === "SHORT" ? "arrowDown" : "arrowUp";
        }
        return meta.shape;
    }

    function markerText(event, meta) {
        if (event.type === "ENTRY") {
            return event.side || meta.label;
        }

        if (event.type === "TAKE_PROFIT" || event.type === "STOP_LOSS") {
            return meta.label;
        }

        if (event.type === "SUPER_TREND_EXIT" || event.type === "STOP_PROMOTION" || event.type === "STOP_PROMOTION_DEFERRED") {
            return meta.label;
        }

        return "";
    }

    function renderTrades(trades) {
        const tradesBody = document.getElementById("trades-body");
        const exitsBody = document.getElementById("exits-body");
        const tradeCount = document.getElementById("trade-count");
        const exitCount = document.getElementById("exit-count");

        tradesBody.replaceChildren();
        exitsBody.replaceChildren();

        if (!Array.isArray(trades) || trades.length === 0) {
            tradesBody.appendChild(emptyRow(11, "거래 내역이 없습니다."));
            exitsBody.appendChild(emptyRow(8, "분할 청산 내역이 없습니다."));
            tradeCount.textContent = "-";
            exitCount.textContent = "-";
            return;
        }

        const tradeFragment = document.createDocumentFragment();
        const exitFragment = document.createDocumentFragment();
        let exitsTotal = 0;

        trades.forEach((trade, index) => {
            const exits = Array.isArray(trade.exits) ? trade.exits : [];
            const stats = summarizeTrade(trade, exits);
            tradeFragment.appendChild(tradeRow(trade, stats, index + 1));

            exits.forEach(exit => {
                exitsTotal += 1;
                exitFragment.appendChild(exitRow(exit, index + 1));
            });
        });

        tradesBody.appendChild(tradeFragment);
        exitsBody.appendChild(exitFragment);

        if (exitsTotal === 0) {
            exitsBody.appendChild(emptyRow(8, "분할 청산 내역이 없습니다."));
        }

        tradeCount.textContent = `${trades.length}건`;
        exitCount.textContent = exitsTotal ? `${exitsTotal}건` : "-";
    }

    function tradeRow(trade, stats, index) {
        const row = document.createElement("tr");
        appendCell(row, index);
        appendCell(row, trade.side || "-");
        appendCell(row, formatDateTime(trade.entryTimeMs));
        appendCell(row, formatNumber(trade.entryPrice));
        appendCell(row, formatNumber(trade.quantity));
        appendCell(row, formatMoney(trade.entryFee));
        appendCell(row, stats.exitCount);
        appendSignedCell(row, stats.grossPnl);
        appendCell(row, formatMoney(stats.totalFee));
        appendSignedCell(row, stats.netPnl);
        appendCell(row, formatMoney(trade.balanceAfter));
        return row;
    }

    function exitRow(exit, tradeIndex) {
        const realizedPnl = Number(exit.grossPnl || 0) - Number(exit.exitFee || 0);
        const row = document.createElement("tr");
        appendCell(row, tradeIndex);
        appendCell(row, formatDateTime(exit.exitTimeMs));
        appendCell(row, exit.exitReason || "-");
        appendCell(row, formatNumber(exit.exitPrice));
        appendCell(row, formatNumber(exit.quantity));
        appendSignedCell(row, exit.grossPnl);
        appendCell(row, formatMoney(exit.exitFee));
        appendSignedCell(row, realizedPnl);
        return row;
    }

    function summarizeTrade(trade, exits) {
        const grossPnl = exits.reduce((sum, exit) => sum + Number(exit.grossPnl || 0), 0);
        const exitFee = exits.reduce((sum, exit) => sum + Number(exit.exitFee || 0), 0);
        const entryFee = Number(trade.entryFee || 0);
        const totalFee = entryFee + exitFee;

        return {
            exitCount: exits.length,
            grossPnl,
            totalFee,
            netPnl: grossPnl - totalFee
        };
    }

    function appendCell(row, text) {
        const cell = document.createElement("td");
        cell.textContent = text == null || text === "" ? "-" : String(text);
        row.appendChild(cell);
    }

    function appendSignedCell(row, value) {
        const cell = document.createElement("td");
        const number = Number(value);
        cell.textContent = formatMoney(number);
        if (number > 0) {
            cell.classList.add("positive");
        } else if (number < 0) {
            cell.classList.add("negative");
        }
        row.appendChild(cell);
    }

    function emptyRow(colspan, text) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.colSpan = colspan;
        cell.className = "empty-table";
        cell.textContent = text;
        row.appendChild(cell);
        return row;
    }

    function showError(error) {
        const panel = document.getElementById("error-panel");
        const status = document.getElementById("error-status");
        const body = document.getElementById("error-body");
        const payload = error && error.payload ? error.payload : error;
        const message = payload && payload.message ? payload.message : "요청 처리 중 오류가 발생했습니다.";

        panel.hidden = false;
        status.textContent = payload && payload.status ? `${payload.status} ${payload.error || ""}`.trim() : "-";
        body.replaceChildren();

        const messageElement = document.createElement("p");
        messageElement.className = "error-message";
        messageElement.textContent = message;
        body.appendChild(messageElement);

        if (payload && payload.fieldErrors && Object.keys(payload.fieldErrors).length > 0) {
            const list = document.createElement("ul");
            list.className = "field-errors";

            Object.entries(payload.fieldErrors).forEach(([field, fieldMessage]) => {
                const item = document.createElement("li");
                item.textContent = `${field}: ${fieldMessage}`;
                list.appendChild(item);
            });

            body.appendChild(list);
        }
    }

    function hideError() {
        const panel = document.getElementById("error-panel");
        panel.hidden = true;
        document.getElementById("error-status").textContent = "-";
        document.getElementById("error-body").replaceChildren();
    }

    function clearOutput() {
        [
            "metric-initial",
            "metric-final",
            "metric-return",
            "metric-trades",
            "metric-win-rate",
            "metric-pnl",
            "metric-fee",
            "metric-mdd"
        ].forEach(id => setMetric(id, "-"));

        document.getElementById("summary-range").textContent = "-";
        document.getElementById("equity-count").textContent = "-";
        document.getElementById("trade-count").textContent = "-";
        document.getElementById("exit-count").textContent = "-";

        disposeChart("price");
        disposeChart("equity");
        showChartPlaceholder(document.getElementById("price-chart"), "실행 결과가 없습니다.");
        showChartPlaceholder(document.getElementById("equity-chart"), "실행 결과가 없습니다.");

        document.getElementById("trades-body").replaceChildren(emptyRow(11, "실행 결과가 없습니다."));
        document.getElementById("exits-body").replaceChildren(emptyRow(8, "실행 결과가 없습니다."));
        hideError();
    }

    function resetToExample() {
        form.reset();
        clearOutput();
        setStatus("idle", "대기", "예시값을 복원했습니다.");
    }

    function setLoading(isLoading) {
        runButton.disabled = isLoading;
        exampleButton.disabled = isLoading;
        runButton.textContent = isLoading ? "실행 중..." : "백테스트 실행";
    }

    function setStatus(state, text, meta) {
        statusDot.className = `status-dot status-${state}`;
        statusText.textContent = text;
        statusMeta.textContent = meta || "";
    }

    function disposeChart(kind) {
        const chartState = charts[kind];
        if (chartState.observer) {
            chartState.observer.disconnect();
            chartState.observer = null;
        }

        if (chartState.chart) {
            chartState.chart.remove();
            chartState.chart = null;
        }
    }

    function showChartPlaceholder(container, text) {
        const placeholder = document.createElement("div");
        placeholder.className = "chart-placeholder";
        placeholder.textContent = text;
        container.replaceChildren(placeholder);
    }

    function uniqueByTime(data) {
        const map = new Map();
        data.forEach(item => {
            map.set(item.time, item);
        });
        return Array.from(map.values()).sort(sortByTime);
    }

    function sortByTime(a, b) {
        return Number(a.time) - Number(b.time);
    }

    function toChartTime(timeMs) {
        return Math.floor(Number(timeMs) / 1000);
    }

    function normalizeChartTime(time) {
        if (isFiniteNumber(time)) {
            return Number(time);
        }

        if (time && Number.isInteger(time.year) && Number.isInteger(time.month) && Number.isInteger(time.day)) {
            return Math.floor(Date.UTC(time.year, time.month - 1, time.day) / 1000);
        }

        return null;
    }

    function isFiniteNumber(value) {
        return value !== null && value !== "" && Number.isFinite(Number(value));
    }

    function formatDateTime(timeMs) {
        if (!isFiniteNumber(timeMs)) {
            return "-";
        }
        return dateTimeFormat.format(new Date(Number(timeMs)));
    }

    function formatNumber(value) {
        return isFiniteNumber(value) ? numberFormat.format(Number(value)) : "-";
    }

    function formatInteger(value) {
        return Number.isInteger(Number(value)) ? numberFormat.format(Number(value)) : "-";
    }

    function formatMoney(value) {
        return isFiniteNumber(value) ? moneyFormat.format(Number(value)) : "-";
    }

    function formatPercent(value) {
        return isFiniteNumber(value) ? `${percentFormat.format(Number(value))}%` : "-";
    }

    function formatSignedPercent(value) {
        if (!isFiniteNumber(value)) {
            return "-";
        }

        const number = Number(value);
        const sign = number > 0 ? "+" : "";
        return `${sign}${percentFormat.format(number)}%`;
    }

    function percentClass(value) {
        const number = Number(value);
        if (number > 0) {
            return "positive";
        }
        if (number < 0) {
            return "negative";
        }
        return "";
    }
})();
