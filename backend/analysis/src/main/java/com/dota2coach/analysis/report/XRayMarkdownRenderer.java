package com.dota2coach.analysis.report;

import com.dota2coach.analysis.metric.ProfileXRay;
import com.dota2coach.analysis.metric.ProfileXRay.HeroItems;
import com.dota2coach.analysis.metric.ProfileXRay.HeroLine;
import com.dota2coach.analysis.metric.ProfileXRay.ItemUse;
import com.dota2coach.analysis.metric.ProfileXRay.LaneLine;
import com.dota2coach.analysis.metric.ProfileXRay.StatBlock;
import com.dota2coach.analysis.metric.WinLoss;
import com.dota2coach.analysis.metric.XRayCalculator;
import com.dota2coach.core.enums.RankBracket;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;

/** Gera o relatório do Raio-X em Markdown, em português. */
public class XRayMarkdownRenderer {

    private static final Locale PT_BR = Locale.of("pt", "BR");
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ZoneId zone;

    public XRayMarkdownRenderer(ZoneId zone) {
        this.zone = zone;
    }

    public String render(ProfileXRay x) {
        StringBuilder md = new StringBuilder();
        header(md, x);
        overview(md, x);
        heroes(md, x);
        lanes(md, x);
        fundamentals(md, x);
        items(md, x);
        meta(md, x);
        dotabuffCheck(md, x);
        return md.toString();
    }

    private void header(StringBuilder md, ProfileXRay x) {
        md.append("# Raio-X de ").append(escape(x.playerName())).append("\n\n");
        md.append("- **Gerado em:** ").append(dateTime(x.collectedAt())).append('\n');
        md.append("- **Medalha:** ").append(medal(x.rankTier())).append('\n');
        md.append("- **Posição principal (informada pelo jogador):** ")
                .append(x.mainPosition() == null ? "não informada" : x.mainPosition().label()).append('\n');
        md.append("- **Fonte:** OpenDota, partidas \"significativas\" (sem Turbo e modos de evento)\n");
        md.append("- **Recorte recente:** últimas ").append(x.recentWindow()).append(" partidas\n\n");
    }

    private void overview(StringBuilder md, ProfileXRay x) {
        var o = x.overview();
        md.append("## Visão geral\n\n");
        md.append("| | Todas | Últimas ").append(x.recentWindow()).append(" |\n|---|---:|---:|\n");
        md.append("| Partidas | ").append(o.allTime().games()).append(" | ").append(o.recent().games()).append(" |\n");
        md.append("| Vitórias | ").append(o.allTime().wins()).append(" | ").append(o.recent().wins()).append(" |\n");
        md.append("| Taxa de vitória | ").append(pct(o.allTime().winRate())).append(" | ")
                .append(pct(o.recent().winRate())).append(" |\n\n");
        md.append("Nas últimas ").append(o.recent().games()).append(" partidas: ")
                .append(o.rankedRecent()).append(" ranqueadas, ")
                .append(o.partyRecent()).append(" em grupo e ")
                .append(o.abandonedRecent()).append(o.abandonedRecent() == 1 ? " abandono. " : " abandonos. ")
                .append(matches(o.last30Days())).append(" nos últimos 30 dias.");
        if (o.firstMatch() != null) {
            md.append(" Histórico de ").append(date(o.firstMatch())).append(" a ").append(date(o.lastMatch()))
                    .append('.');
        }
        md.append("\n\n");
    }

    private void heroes(StringBuilder md, ProfileXRay x) {
        md.append("## Heróis (últimas ").append(x.recentWindow()).append(")\n\n");
        md.append("Pool: **").append(x.heroPoolSize()).append(" heróis** com ")
                .append(XRayCalculator.POOL_MIN_GAMES).append(" ou mais partidas no recorte.\n\n");
        if (x.heroes().isEmpty()) {
            md.append("_Sem partidas no recorte._\n\n");
            return;
        }
        md.append("| Herói | Partidas | Vitória | KDA | GPM | XPM | Histórico | Vitória do herói na faixa | Presença na faixa |\n");
        md.append("|---|---:|---:|---:|---:|---:|---:|---:|---:|\n");
        for (HeroLine h : x.heroes()) {
            md.append("| ").append(escape(h.name()))
                    .append(" | ").append(h.recent().games())
                    .append(" | ").append(pct(h.recent().winRate()))
                    .append(" | ").append(dec(h.kda()))
                    .append(" | ").append(whole(h.goldPerMin()))
                    .append(" | ").append(whole(h.xpPerMin()))
                    .append(" | ").append(h.allTime().games()).append(" (").append(pct(h.allTime().winRate())).append(')')
                    .append(" | ").append(h.bracketWinRate() == null ? "—" : pct(h.bracketWinRate()))
                    .append(" | ").append(h.bracketPickRate() == null ? "—" : pct(h.bracketPickRate()))
                    .append(" |\n");
        }
        md.append('\n');
    }

    private void lanes(StringBuilder md, ProfileXRay x) {
        md.append("## Rotas\n\n");
        md.append("Rota conhecida em ").append(pct(x.laneCoverage()))
                .append(" das partidas recentes (só aparece quando o replay foi analisado).\n\n");
        if (x.lanes().isEmpty()) {
            md.append("_Nenhuma partida recente com replay analisado. Rode de novo depois que a OpenDota terminar as análises pedidas._\n\n");
            return;
        }
        md.append("| Rota | Partidas | Vitória |\n|---|---:|---:|\n");
        for (LaneLine l : x.lanes()) {
            md.append("| ").append(l.lane().label()).append(" | ").append(l.result().games())
                    .append(" | ").append(pct(l.result().winRate())).append(" |\n");
        }
        md.append('\n');
    }

    private void fundamentals(StringBuilder md, ProfileXRay x) {
        StatBlock latest = x.fundamentals().latest();
        StatBlock previous = x.fundamentals().previous();
        md.append("## Fundamentos\n\n");
        md.append("| Métrica | Últimas ").append(latest.matches()).append(" | ")
                .append(previous.matches()).append(" anteriores | Variação |\n|---|---:|---:|---:|\n");
        row(md, "Taxa de vitória", latest, previous, StatBlock::winRate, true);
        row(md, "Ouro por minuto", latest, previous, StatBlock::goldPerMin, false);
        row(md, "XP por minuto", latest, previous, StatBlock::xpPerMin, false);
        row(md, "Last hits por minuto", latest, previous, StatBlock::lastHitsPerMin, false);
        row(md, "Mortes por partida", latest, previous, StatBlock::deaths, false);
        row(md, "KDA", latest, previous, StatBlock::kda, false);
        row(md, "Dano em heróis por minuto", latest, previous, StatBlock::heroDamagePerMin, false);
        row(md, "Duração média (min)", latest, previous, StatBlock::durationMinutes, false);
        md.append('\n');
    }

    private void row(StringBuilder md, String label, StatBlock latest, StatBlock previous,
            ToDoubleFunction<StatBlock> value, boolean percent) {
        double a = value.applyAsDouble(latest);
        double b = value.applyAsDouble(previous);
        md.append("| ").append(label)
                .append(" | ").append(percent ? pct(a) : dec(a))
                .append(" | ").append(previous.matches() == 0 ? "—" : percent ? pct(b) : dec(b))
                .append(" | ").append(previous.matches() == 0 || latest.matches() == 0 ? "—"
                        : percent ? signedPoints(a - b) : signed(a - b))
                .append(" |\n");
    }

    private void items(StringBuilder md, ProfileXRay x) {
        md.append("## Itens\n\n");
        if (x.items().isEmpty()) {
            md.append("_Sem partidas com itens suficientes nos heróis mais jogados._\n\n");
            return;
        }
        md.append("Itens do inventário final que custam ").append(XRayCalculator.CORE_ITEM_MIN_COST)
                .append(" de ouro ou mais, comparados com os mais comprados no herói por todos os jogadores.\n\n");
        for (HeroItems hi : x.items()) {
            md.append("### ").append(escape(hi.heroName())).append("\n\n");
            md.append("- **Partidas analisadas:** ").append(hi.matchesWithItems()).append('\n');
            md.append("- **Aderência:** ").append(pct(hi.adherence()))
                    .append(" dos seus itens principais estão entre os populares do herói\n");
            md.append("- **Seus itens mais frequentes:** ").append(hi.playerTop().stream()
                    .map(XRayMarkdownRenderer::itemUse).collect(Collectors.joining(", "))).append('\n');
            md.append("- **Populares no herói:** ").append(hi.metaTop().isEmpty() ? "—"
                    : String.join(", ", hi.metaTop())).append('\n');
            md.append("- **Populares que você quase não termina com:** ")
                    .append(hi.missingMeta().isEmpty() ? "nenhum" : String.join(", ", hi.missingMeta()))
                    .append("\n\n");
        }
    }

    private static String itemUse(ItemUse use) {
        return escape(use.name()) + " (" + use.matches() + (use.meta() ? ", popular" : "") + ")";
    }

    private void meta(StringBuilder md, ProfileXRay x) {
        md.append("## Meta\n\n");
        String faixa = x.bracket() == null ? "em todas as faixas (você está sem medalha)"
                : "na faixa " + x.bracket().label();
        md.append(pct(x.shareOnWinningMetaHeroes()))
                .append(" das suas partidas recentes foram com heróis que vencem 50% ou mais ")
                .append(faixa).append(".\n\n");
    }

    private void dotabuffCheck(StringBuilder md, ProfileXRay x) {
        md.append("## Conferência com o Dotabuff\n\n");
        md.append("Compare com o seu perfil no Dotabuff. Pequenas diferenças são esperadas porque aqui ficam de fora Turbo e modos de evento.\n\n");
        WinLoss all = x.overview().allTime();
        md.append("- [ ] Total de partidas: ").append(all.games()).append(", taxa de vitória ")
                .append(pct(all.winRate())).append('\n');
        x.heroes().stream().limit(3).forEach(h -> md.append("- [ ] ").append(escape(h.name())).append(": ")
                .append(matches(h.allTime().games())).append(" no histórico, vitória ")
                .append(pct(h.allTime().winRate())).append('\n'));
        md.append('\n');
    }

    static String matches(int count) {
        return count + (count == 1 ? " partida" : " partidas");
    }

    static String medal(Integer rankTier) {
        return RankBracket.fromRankTier(rankTier)
                .map(b -> b == RankBracket.IMMORTAL ? b.label() : b.label() + " " + (rankTier % 10))
                .orElse("sem medalha");
    }

    private String date(Instant instant) {
        return DATE.format(instant.atZone(zone));
    }

    private String dateTime(Instant instant) {
        return DATE_TIME.format(instant.atZone(zone));
    }

    static String pct(double fraction) {
        NumberFormat f = NumberFormat.getNumberInstance(PT_BR);
        f.setMinimumFractionDigits(1);
        f.setMaximumFractionDigits(1);
        return f.format(fraction * 100) + "%";
    }

    static String dec(double value) {
        NumberFormat f = NumberFormat.getNumberInstance(PT_BR);
        f.setMinimumFractionDigits(1);
        f.setMaximumFractionDigits(1);
        return f.format(value);
    }

    private static String whole(double value) {
        return String.valueOf(Math.round(value));
    }

    private static String signed(double value) {
        return (value > 0 ? "+" : "") + dec(value);
    }

    private static String signedPoints(double fractionDelta) {
        return (fractionDelta > 0 ? "+" : "") + dec(fractionDelta * 100) + " p.p.";
    }

    /** Evita que nomes com | ou caracteres de Markdown quebrem as tabelas. */
    static String escape(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\\", "\\\\").replace("|", "\\|").replace("*", "\\*").replace("_", "\\_")
                .replace("<", "&lt;").replace(">", "&gt;");
    }
}
