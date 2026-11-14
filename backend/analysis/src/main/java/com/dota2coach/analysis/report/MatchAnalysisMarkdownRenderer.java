package com.dota2coach.analysis.report;

import static com.dota2coach.analysis.report.XRayMarkdownRenderer.dec;
import static com.dota2coach.analysis.report.XRayMarkdownRenderer.escape;
import static com.dota2coach.analysis.report.XRayMarkdownRenderer.pct;
import static com.dota2coach.analysis.report.XRayMarkdownRenderer.percentile;

import com.dota2coach.analysis.metric.MatchAnalysis;
import com.dota2coach.analysis.metric.MatchAnalysis.Context;
import com.dota2coach.analysis.metric.MatchAnalysis.DamageShare;
import com.dota2coach.analysis.metric.MatchAnalysis.DeathLine;
import com.dota2coach.analysis.metric.MatchAnalysis.DeathPhaseLine;
import com.dota2coach.analysis.metric.MatchAnalysis.FightLine;
import com.dota2coach.analysis.metric.MatchAnalysis.ItemLine;
import com.dota2coach.analysis.metric.MatchAnalysis.LaneOpponent;
import com.dota2coach.analysis.metric.MatchAnalysis.Laning;
import com.dota2coach.analysis.metric.MatchAnalysis.Line;
import com.dota2coach.analysis.metric.MatchAnalysis.Matchup;
import com.dota2coach.analysis.metric.MatchAnalysis.ObjectiveLine;
import com.dota2coach.analysis.metric.MatchAnalysis.PhasePower;
import com.dota2coach.analysis.metric.MatchAnalysis.Pick;
import com.dota2coach.analysis.metric.MatchAnalysis.PlayerItems;
import com.dota2coach.analysis.metric.MatchAnalysis.TeamGold;
import com.dota2coach.analysis.metric.MatchAnalysis.TeamProfile;
import com.dota2coach.analysis.metric.MatchAnalysis.Timeline;
import com.dota2coach.analysis.metric.MatchAnalysis.Usual;
import com.dota2coach.core.enums.BenchmarkMetric;
import com.dota2coach.core.enums.Lane;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
        context(md, a);
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
        playersItems(md, a);
        vision(md, a);
        teamGold(md, a);
        objectives(md, a);
        fights(md, a);
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

    private static void context(StringBuilder md, MatchAnalysis a) {
        Context c = a.context();
        if (c.teams().isEmpty()) {
            return;
        }
        md.append("## Contexto da partida\n\n");
        draft(md, c);
        md.append("### Composição dos times\n\n");
        TeamProfile yours = c.teams().get(0);
        TeamProfile enemy = c.teams().get(1);
        md.append("| | Seu time").append(yours.won() ? " (venceu)" : "").append(" | Time adversário")
                .append(enemy.won() ? " (venceu)" : "").append(" |\n|---|---|---|\n");
        md.append("| Heróis | ").append(heroes(yours)).append(" | ").append(heroes(enemy)).append(" |\n");
        if (yours.damageDealt().total() > 0 || enemy.damageDealt().total() > 0) {
            md.append("| Dano causado em heróis | ").append(damage(yours.damageDealt())).append(" | ")
                    .append(damage(enemy.damageDealt())).append(" |\n");
        }
        md.append("| Funções dos heróis | ").append(roles(yours)).append(" | ").append(roles(enemy)).append(" |\n\n");
        if (c.damageReceivedByYou().total() > 0) {
            md.append("Dano que você recebeu de heróis: ").append(damage(c.damageReceivedByYou())).append(".\n\n");
        }
        md.append("Funções são as oficiais de cada herói (não a posição de quem jogou). No dano, ataque básico conta ")
                .append("como físico, e \"outro\" junta itens e habilidades sem tipo conhecido.\n\n");
        if (!c.matchups().isEmpty()) {
            md.append("### Seu herói contra os adversários\n\n");
            md.append("Taxa de vitória do ").append(escape(a.heroName()))
                    .append(" contra cada herói em partidas públicas recentes (todas as faixas).\n\n");
            md.append("| Adversário | Vitórias do ").append(escape(a.heroName())).append(" | Partidas |\n")
                    .append("|---|---:|---:|\n");
            for (Matchup m : c.matchups()) {
                md.append("| ").append(escape(m.heroName())).append(m.laneOpponent() ? " (sua rota)" : "")
                        .append(" | ").append(pct(m.winRate())).append(" | ").append(m.games()).append(" |\n");
            }
            md.append('\n');
        }
        if (!c.power().isEmpty()) {
            md.append("### Força dos times por duração da partida\n\n");
            md.append("Taxa de vitória média dos 5 heróis de cada time em partidas públicas recentes que duraram ")
                    .append("esse tempo. Mostra que time tende a ficar mais forte com o passar do jogo.\n\n");
            md.append("| Duração | Seu time | Time adversário |\n|---|---:|---:|\n");
            for (PhasePower p : c.power()) {
                md.append("| ").append(p.phase()).append(" | ").append(p.yourTeam() == null ? NONE : pct(p.yourTeam()))
                        .append(" | ").append(p.enemyTeam() == null ? NONE : pct(p.enemyTeam())).append(" |\n");
            }
            md.append('\n');
        }
    }

    private static void draft(StringBuilder md, Context c) {
        if (!c.pickOrderKnown()) {
            md.append("A OpenDota não informou a ordem dos picks desta partida.\n\n");
            return;
        }
        md.append("### Draft\n\n");
        md.append("| Pick | Time | Herói |\n|---:|---|---|\n");
        for (Pick p : c.picks()) {
            md.append("| ").append(p.order()).append(" | ").append(p.yourTeam() ? "seu" : "adversário").append(" | ")
                    .append(p.you() ? "**" + escape(p.heroName()) + " (você)**" : escape(p.heroName()))
                    .append(" |\n");
        }
        md.append('\n');
        if (c.yourTeamPickNumber() != null) {
            md.append("Você foi o ").append(c.yourTeamPickNumber()).append("º pick do seu time")
                    .append(c.yourTeamPickNumber() == 5 ? " (último, já vendo os 5 adversários)" : "")
                    .append(".\n\n");
        }
    }

    private static String heroes(TeamProfile team) {
        return team.heroes().stream().map(XRayMarkdownRenderer::escape).collect(Collectors.joining(", "));
    }

    private static String roles(TeamProfile team) {
        return team.roles().isEmpty() ? NONE
                : team.roles().stream().map(r -> r.role() + " " + r.heroes()).collect(Collectors.joining(", "));
    }

    static String damage(DamageShare d) {
        if (d.total() == 0) {
            return NONE;
        }
        StringBuilder text = new StringBuilder("físico ").append(pct(d.physical())).append(", mágico ")
                .append(pct(d.magical())).append(", puro ").append(pct(d.pure()));
        if (d.other() > 0) {
            text.append(", outro ").append(pct(d.other()));
        }
        return text.toString();
    }

    private static void percentiles(StringBuilder md, MatchAnalysis a) {
        if (a.percentiles().isEmpty() && a.lastHitsAt10Percentile() == null) {
            return;
        }
        md.append("## Comparação com outros jogadores do herói\n\n");
        md.append("Percentil entre as partidas públicas recentes do herói, calculado pela OpenDota: p50 é a ")
                .append("mediana, p90 quer dizer acima de 90% das partidas. Em mortes, percentil alto é ruim: ")
                .append("quer dizer que morreu mais que a maioria.\n\n");
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

    private static void playersItems(StringBuilder md, MatchAnalysis a) {
        if (a.playersItems().isEmpty()) {
            return;
        }
        md.append("## Itens dos dois times\n\n");
        md.append("Primeira compra de cada item principal. \"Populares que não fez\" são os itens principais mais ")
                .append("comprados com o herói em partidas públicas recentes, por fase do jogo, que o jogador não ")
                .append("comprou nesta partida. Não ter feito um deles não é erro por si só.\n\n");
        md.append("| Time | Herói | Itens principais | Populares que não fez |\n|---|---|---|---|\n");
        for (PlayerItems p : a.playersItems()) {
            md.append("| ").append(p.yourTeam() ? "seu" : "adversário").append(" | ")
                    .append(p.you() ? "**" + escape(p.heroName()) + " (você)**" : escape(p.heroName())).append(" | ")
                    .append(p.items().isEmpty() ? "nenhum"
                            : p.items().stream().map(i -> escape(i.itemName()) + " " + clock(i.timeSeconds()))
                                    .collect(Collectors.joining(", ")))
                    .append(" | ")
                    .append(!p.popularKnown() ? NONE
                            : p.popularNotBuilt().isEmpty() ? "fez todos"
                            : p.popularNotBuilt().stream().map(XRayMarkdownRenderer::escape)
                                    .collect(Collectors.joining(", ")))
                    .append(" |\n");
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

    private static void objectives(StringBuilder md, MatchAnalysis a) {
        if (a.timeline().objectives().isEmpty()) {
            return;
        }
        md.append("## Objetivos\n\n");
        long towersYours = a.timeline().objectives().stream()
                .filter(o -> o.yourTeam() && o.event().contains("torre")).count();
        long towersEnemy = a.timeline().objectives().stream()
                .filter(o -> !o.yourTeam() && o.event().contains("torre")).count();
        md.append("Torres derrubadas: seu time ").append(towersYours).append(", adversário ").append(towersEnemy)
                .append(".\n\n");
        md.append("| Tempo | Quem | O quê |\n|---|---|---|\n");
        for (ObjectiveLine o : a.timeline().objectives()) {
            md.append("| ").append(clock(o.timeSeconds())).append(" | ")
                    .append(o.yourTeam() ? "Seu time" : "Adversário").append(" | ").append(o.event()).append(" |\n");
        }
        md.append('\n');
    }

    private static void fights(StringBuilder md, MatchAnalysis a) {
        Timeline t = a.timeline();
        if (t.fightsTotal() == 0) {
            return;
        }
        md.append("## Lutas que mais mexeram no ouro\n\n");
        md.append("A OpenDota detectou ").append(t.fightsTotal()).append(t.fightsTotal() == 1 ? " luta" : " lutas")
                .append(" com 3 ou mais mortes. Seu time ganhou mais ouro que o adversário em ")
                .append(t.fightsWonByGold()).append(", e você participou de ").append(t.fightsWithYou())
                .append(". A luta que não terminou antes do fim do jogo não entra.\n\n");
        md.append("| Tempo | Ouro para seu time | Mortes do seu time | Mortes do adversário | Você |\n")
                .append("|---|---:|---|---|---|\n");
        for (FightLine f : t.biggestFights()) {
            md.append("| ").append(clock(f.startSeconds())).append(" a ").append(clock(f.endSeconds())).append(" | ")
                    .append(f.goldSwing() > 0 ? "+" : "").append(f.goldSwing()).append(" | ")
                    .append(heroes(f.yourTeamDead()))
                    .append(f.buybacks() > 0 ? " (" + f.buybacks() + " buyback" + (f.buybacks() > 1 ? "s" : "") + ")"
                            : "")
                    .append(" | ").append(heroes(f.enemiesDead())).append(" | ")
                    .append(!f.youTookPart() ? "não participou" : f.youDied() ? "participou e morreu" : "participou")
                    .append(" |\n");
        }
        md.append('\n');
    }

    /** Heróis agrupados, ex.: "Slark (2), Zeus", ou "nenhuma". */
    private static String heroes(List<String> names) {
        if (names.isEmpty()) {
            return "nenhuma";
        }
        Map<String, Long> counts = names.stream()
                .collect(Collectors.groupingBy(n -> n, LinkedHashMap::new, Collectors.counting()));
        return counts.entrySet().stream()
                .map(e -> escape(e.getKey()) + (e.getValue() > 1 ? " (" + e.getValue() + ")" : ""))
                .collect(Collectors.joining(", "));
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
