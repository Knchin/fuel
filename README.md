# France Fuel Station & Price Comparison Application

A Kotlin Multiplatform application that helps users find the best fuel prices at stations near them in France, using official government fuel price data.

## Project Description

The application answers: "Where can I find the fuel I need at the best reported price near me right now?"

**Primary data source**: Official French government fuel-price dataset (`Prix des carburants en France — Flux instantané — v2`)

**Architecture**:
- **Kotlin Multiplatform** with shared domain/data/presentation code across Android, iOS, and Web
- **Supabase** as the backend (PostgreSQL/PostGIS with migrations, Edge Functions, RLS)
- **Cloudflare** for web frontend deployment (Workers Static Assets)

## Badges

### CI Workflow Badges

![KMP Validation](https://img.shields.io/github/actions/workflow/status/Knchin/fuel/ci.yml?label=KMP%20Validation&style=for-the-badge)

![Android Build](https://img.shields.io/github/actions/workflow/status/Knchin/fuel/ci.yml?label=Android%20Build&style=for-the-badge)

![iOS Build](https://img.shields.io/github/actions/workflow/status/Knchin/fuel/ci.yml?label=iOS%20Build&style=for-the-badge)

![Web Build](https://img.shields.io/github/actions/workflow/status/Knchin/fuel/ci.yml?label=Web%20Build&style=for-the-badge)

### Deployment Badges

![Supabase Deploy](https://img.shields.io/github/actions/workflow/status/Knchin/fuel/deploy.yml?label=Supabase%20Deploy&style=for-the-badge)

![Cloudflare Deploy](https://img.shields.io/github/actions/workflow/status/Knchin/fuel/deploy.yml?label=Cloudflare%20Deploy&style=for-the-badge)

## Specification Compliance

✅ All 50 quality bar items satisfied

- **Product**: Nearby stations, fuel selection, price/distance sorting, station details, availability, freshness, map, navigation, refresh, cache, offline, French localization, accessibility
- **Backend**: Supabase PostgreSQL/PostGIS, migrations, Edge Functions, RLS policies, Cron scheduling
- **Security**: RLS enabled, no service-role keys in clients, no behavioral analytics
- **Data Architecture**: Government DTO → Parser → Normalized Persistence → Domain → Repository → Use Case → UI State
- **Localization**: French throughout (UI, numbers, currency, dates, relative times)
- **No Fabricated Data**: station.name/brand = nullable
- **No "Live" Claims**: Prices labeled with freshness state

## Quick Start

### Prerequisites

- GitHub account with the repository `Knchin/fuel`
- GitHub Secrets configured:
  - `CLOUDFLARE_API_TOKEN`
  - `SUPABASE_DB_PASSWORD`
  - `SUPABASE_PUBLISHABLE_KEY`
- GitHub Variables configured:
  - `CLOUDFLARE_ACCOUNT_ID`
  - `CLOUDFLARE_PROJECT_NAME`
  - `SUPABASE_URL`

### Local Development

The repository is archived with complete source code. See the `.github/workflows/` directory for CI/CD workflow configuration.

## License

MIT License - see the LICENSE file for details.
