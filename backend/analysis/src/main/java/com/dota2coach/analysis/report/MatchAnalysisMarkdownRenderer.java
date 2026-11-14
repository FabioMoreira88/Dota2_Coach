package com.dota2coach.analysis.report;

import static com.dota2coach.analysis.report.XRayMarkdownRenderer.dec;
import static com.dota2coach.analysis.report.XRayMarkdownRenderer.escape;
import static com.dota2coach.analysis.report.XRayMarkdownRenderer.pct;
import static com.dota2coach.analysis.report.XRayMarkdownRenderer.percentile;

import com.dota2coach.analysis.metric.MatchAnalysis;
import com.dota2coach.analysis.metric.MatchAnalysis.DeathLine;
import com.dota2coach.analysis.metric.MatchAnalysis.DeathPhaseLine;
import com.dota2coach.analysis.metric.MatchAnalysis.ItemLine;
import com.dota2coach.analysis.metric.MatchAnalysis.LaneOpponent;
import com.dota2coach.analysis.metric.MatchAnalysis.Laning;
import com.dota2coach.analysis.metric.MatchAnalysis.Line;
import com.dota2coach.analysis.metric.MatchAnalysis.TeamGold;
import com.dota2coach.analysis.metric.MatchAnalysis.Usual;
import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.Lane;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

/** Gera o relatório de uma partida escolhida em Markdown, em português. */
public class MatchAnalysisMarkdownRenderer {

    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final String NONE = "–";

    private final ZoneId zone;

    public MatchAnalysisMarkdownRenderer(ZoneId zone) {
        this.zone = zone;
    }

    public String render(MatchAnalysis a) {
        StringBuilder md = new StringBuilder();
        header(md, a);
        summary(md, a);
        if (!a.replayParsed()) {
            md.append("## Replay ainda não analisado\n\n")
                    .append("A OpenDota ainda não analisou o replay desta partida, então fase de rotas, mortes, ")
                    .append("itens e visão ficam de fora. A análise foi pedida: rode de novo em uns 15 minutos.\n");
            return md.toString();
        }
        percentiles(md, a);
        laning(md, a);
        deaths(md, a);
        items(md, a);
        vision(md, a);
        teamGold(md, a);
        return md.toString();
    }

    private void header(StringBuilder md, MatchAnalysis a) {
        md.append("# Análise da partida ").append(a.matchId()).append("\n\n");
        md.append("- **Herói:** ").append(escape(a.heroName())).append('\n');
        md.append("- **Resultado:** ").append(a.won() ? "vitória" : "derrota").append(" em ")
                .append(minutes(a.duration().toSeconds())).append('\n');
        md.append("- **Data:** ").append(DATE_TIME.format(a.startTime().atZone(zone))).append(" (")
                .append(a.gameMode().label()).append(a.ranked() ? ", ranqueada" : ", não ranqueada").append(")\n");
        md.append("- **Posição principal (informada pelo jogador):** ")
                .append(a.position() == null ? "não informada" : a.position().label()).append('\n');
        if (a.lane() != Lane.UNKNOWN) {
            md.append("- **Rota nesta partida:** ").append(a.lane().label()).append('\n');
        }
        md.append("- **Link:** https://www.opendota.com/matches/").append(a.matchId()).append("\n\n");
    }

    private void summary(StringBuilder md, MatchAnalysis a) {
        Line l = a.line();
        Usual u = a.usual();
        boolean withUsual = u != null && u.heroGames() > 0;
        md.append("## Resumo\n\n");
        if (withUsual) {
            md.append("| | Nesta partida | Seu normal com o herói (").append(u.heroGames())
                    .append(" partidas recentes) |\n|---|---:|---:|\n");
        } else {
            md.append("| | Nesta partida |\n|---|---:|\n");
        }
        row(md, withUsual, "Abates / mortes / assistências", l.kills() + " / " + l.deaths() + " / " + l.assists(),
                null);
        double kda = (l.kills() + l.assists()) / (double) Math.max(1, l.deaths());
        row(md, withUsual, "KDA", dec(kda), withUsual ? dec(orZero(u.heroKda())) : null);
        row(md, withUsual, "Ouro/min", String.valueOf(l.goldPerMin()),
                withUsual ? String.valueOf(Math.round(orZero(u.heroGoldPerMin()))) : null);
        row(md, withUsual, "XP/min", String.valueOf(l.xpPerMin()),
                withUsual ? String.valueOf(Math.round(orZero(u.heroXpPerMin()))) : null);
        row(md, withUsual, "Last hits / denies", l.lastHits() + " / " + l.denies(), null);
        row(md, withUsual, "Dano em heróis", String.valueOf(l.heroDamage()), null);
        row(md, withUsual, "Dano em torres", String.valueOf(l.towerDamage()), null);
        if (l.heroHealing() > 0) {
            row(md, withUsual, "Cura", String.valueOf(l.heroHealing()), null);
        }
        row(md, withUsual, "Patrimônio final", String.valueOf(l.netWorth()), null);
        if (a.killParticipation() != null) {
            row(md, withUsual, "Participação nos abates do time", pct(a.killParticipation()), null);
        }
        md.append('\n');
        if (u == null) {
            md.append("Ainda não há Raio-X salvo desta conta, então a partida não foi comparada com o seu ")
                    .append("padrão.\n\n");
        } else if (u.heroGames() == 0) {
            md.append("O herói não aparece nas partidas recentes do último Raio-X, então a comparação usa só os ")
                    .append("números gerais.\n\n");
        }
    }

    private static void row(StringBuilder md, boolean withUsual, String label, String value, String usual) {
        md.append("| ").append(label).append(" | ").append(value).append(" |");
        if (withUsual) {
            md.append(' ').append(usual == null ? NONE : usual).append(" |");
        }
        md.append('\n');
    }

    private static void percentiles(StringBuilder md, MatchAnalysis a) {
        if (a.percentiles().isEmpty() && a.lastHitsAt10Percentile() == null) {
            return;
        }
        md.append("## Comparação com outros jogadores do herói\n\n");
        md.append("Percentil entre as partidas públicas recentes do herói, calculado pela OpenDota: p50 é a ")
                .append("mediana, p90 quer dizer melhor que 90% das partidas.\n\n");
        md.append("| Métrica | Percentil |\n|---|---:|\n");
        for (BenchmarkMetric metric : BenchmarkMetric.values()) {
            Double value = a.percentiles().get(metric);
            if (value != null) {
                md.append("| ").append(metric.label()).append(" | ").append(percentile(value)).append(" |\n");
            }
        }
        if (a.lastHitsAt10Percentile() != null) {
            md.append("| Last hits aos 10 min | ").append(percentile(a.lastHitsAt10Percentile())).append(" |\n");
        }
        md.append('\n');
    }

    private static void laning(StringBuilder md, MatchAnalysis a) {
        Laning l = a.laning();
        if (l.lastHitsAt10() == null) {
            return;
        }
        md.append("## Fase de rotas (10 minutos)\n\n");
        md.append("| Herói | Last hits | Denies | Ouro | XP |\n|---|---:|---:|---:|---:|\n");
        md.append("| **").append(escape(a.heroName())).append(" (você)** | ").append(value(l.lastHitsAt10()))
                .append(" | ").append(value(l.deniesAt10())).append(" | ").append(value(l.goldAt10()))
                .append(" | ").append(value(l.xpAt10())).append(" |\n");
        for (LaneOpponent o : l.opponents()) {
            md.append("| ").append(escape(o.heroName())).append(" (adversário) | ").append(value(o.lastHitsAt10()))
                    .append(" | ").append(value(o.deniesAt10())).append(" | ").append(value(o.goldAt10()))
                    .append(" | ").append(value(o.xpAt10())).append(" |\n");
        }
        md.append('\n');
        if (l.opponents().isEmpty()) {
            md.append("A OpenDota não identificou adversários na sua rota.\n\n");
        }
        if (l.laneEfficiencyPct() != null) {
            md.append("Eficiência na fase de rotas (OpenDota): ").append(l.laneEfficiencyPct()).append("%.\n\n");
        }
        Usual u = a.usual();
        if (u != null && u.lastHitsAt10() != null) {
            md.append("Seu normal: ").append(dec(u.lastHitsAt10())).append(" last hits aos 10 minutos")
                    .append(u.lastHitsAt10ForHero() ? " com o herói" : " (média geral)");
            if (u.deniesAt10() != null) {
                md.append(" e ").append(dec(u.deniesAt10())).append(" denies (média geral)");
            }
            md.append(".\n\n");
        }
    }

    private static void deaths(StringBuilder md, MatchAnalysis a) {
        md.append("## Mortes\n\n");
        if (a.deaths().isEmpty()) {
            md.append("Nenhuma morte nesta partida.\n\n");
        } else {
            md.append("| Tempo | Fase | Quem matou | Tempo morto |\n|---|---|---|---:|\n");
            for (DeathLine d : a.deaths()) {
                md.append("| ").append(clock(d.timeSeconds())).append(" | ").append(d.phase()).append(" | ")
                        .append(d.killer() == null ? "não identificado" : escape(d.killer())).append(" | ")
                        .append(d.secondsDead()).append(" s |\n");
            }
            int secondsDead = a.deaths().stream().mapToInt(DeathLine::secondsDead).sum();
            md.append("\nTempo morto no total: ").append(minutes(secondsDead));
            if (a.usual() != null && a.usual().secondsDeadPerMatch() > 0) {
                md.append(" (seu normal: ").append(minutes(Math.round(a.usual().secondsDeadPerMatch())))
                        .append(" por partida)");
            }
            md.append(".\n\n");
        }
        boolean withUsual = a.deathsByPhase().stream().anyMatch(p -> p.usualPerMatch() != null);
        if (!a.deathsByPhase().isEmpty() && withUsual) {
            md.append("| Fase | Mortes nesta partida | Seu normal por partida |\n|---|---:|---:|\n");
            for (DeathPhaseLine p : a.deathsByPhase()) {
                md.append("| ").append(p.phase()).append(" | ").append(p.deaths()).append(" | ")
                        .append(p.usualPerMatch() == null ? NONE : dec(p.usualPerMatch())).append(" |\n");
            }
            md.append('\n');
        }
    }

    private static void items(StringBuilder md, MatchAnalysis a) {
        if (a.items().isEmpty()) {
            return;
        }
        md.append("## Itens principais\n\n");
        boolean withUsual = a.items().stream().anyMatch(i -> i.usualMinute() != null);
        md.append(withUsual ? "| Item | Comprado aos | Seu normal com o herói |\n|---|---:|---:|\n"
                : "| Item | Comprado aos |\n|---|---:|\n");
        for (ItemLine i : a.items()) {
            md.append("| ").append(escape(i.itemName())).append(" | ").append(clock(i.timeSeconds())).append(" |");
            if (withUsual) {
                md.append(' ').append(i.usualMinute() == null ? NONE : dec(i.usualMinute()) + " min").append(" |");
            }
            md.append('\n');
        }
        md.append('\n');
    }

    private static void vision(StringBuilder md, MatchAnalysis a) {
        md.append("## Visão\n\n");
        md.append("Wards colocadas: ").append(a.observersPlaced()).append(" observer e ").append(a.sentriesPlaced())
                .append(" sentry.\n\n");
    }

    private static void teamGold(StringBuilder md, MatchAnalysis a) {
        if (a.teamGold().isEmpty()) {
            return;
        }
        md.append("## Ouro do seu time contra o adversário\n\n");
        md.append("| Minuto | Vantagem |\n|---:|---:|\n");
        for (TeamGold g : a.teamGold()) {
            md.append("| ").append(g.minute()).append(" | ").append(g.advantage() > 0 ? "+" : "")
                    .append(g.advantage()).append(" |\n");
        }
        md.append('\n');
    }

    private static String value(Integer value) {
        return value == null ? NONE : value.toString();
    }

    private static double orZero(Double value) {
        return value == null ? 0 : value;
    }

    /** Tempo de jogo como m:ss (negativo antes do início, ex.: -1:20). */
    static String clock(int seconds) {
        int abs = Math.abs(seconds);
        return (seconds < 0 ? "-" : "") + abs / 60 + ":" + String.format("%02d", abs % 60);
    }

    static String minutes(long seconds) {
        return seconds / 60 + " min " + String.format("%02d", seconds % 60) + " s";
    }
}
