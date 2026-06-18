# Nexus Plus — Play Store Release Assets

## Icons
- `play_store_icon_512.png` — 512×512 Play Store icon (required)
- `NexusPlus/app/src/main/res/mipmap-*/ic_launcher_icon.png` — launcher PNG icons (all densities)

## Screenshots (Phone — 9:16)
- `screenshot_home.png` — Home screen feature tiles
- `screenshot_ai_features.png` — AI features & Social Media
- `screenshot_security.png` — Security features

## Feature Graphic (16:9)
- `feature_graphic.png` — 1024×500 Play Store feature graphic

## Firebase Remote Config Keys (set these in Firebase Console → Remote Config)
| Key | Type | What it controls |
|-----|------|-----------------|
| `google_signin_enabled` | Boolean | Show/hide Google Sign-In button |
| `instagram_url` | String | Instagram link |
| `facebook_url` | String | Facebook link |
| `twitter_url` | String | X (Twitter) link |
| `youtube_url` | String | YouTube channel |
| `tiktok_url` | String | TikTok link |
| `telegram_url` | String | Telegram channel |
| `whatsapp_url` | String | WhatsApp channel |
| `discord_url` | String | Discord server |
| `github_url` | String | GitHub profile |
| `official_website_url` | String | Official website |
| `support_email` | String | Support email address |
| `contact_email` | String | Contact email address |
| `terms_url` | String | Terms & Conditions URL |
| `privacy_url` | String | Privacy Policy URL |
| `update_dialog_enabled` | Boolean | Show update prompt |
| `update_dialog_title` | String | Update dialog title |
| `update_dialog_message` | String | Update dialog message |
| `update_dialog_url` | String | Play Store URL for update |
| `app_announcement_enabled` | Boolean | Show announcement banner |
| `app_announcement` | String | Announcement text |
| `feature_aira_premium` | Boolean | Lock Aira AI as premium |
| `feature_biometric_vault_premium` | Boolean | Lock Biometric Vault |
| `feature_health_vault_premium` | Boolean | Lock Health Vault |
| `feature_nse_premium` | Boolean | Lock NSE (speech engine) |
| `feature_nexus_ott_premium` | Boolean | Lock Nexus OTT |
| `feature_image_editor_premium` | Boolean | Lock Smart Image Editor |
| `feature_ai_image_premium` | Boolean | Lock AI Image Generator |
| `feature_expense_tracker_premium` | Boolean | Lock Expense Tracker |
| `feature_voice_recorder_premium` | Boolean | Lock Voice Recorder |
