# Baity Sync Protocol / Baity 远端同步协议

This document describes how Baity performs remote synchronization and what data it exchanges. 
这个文档说明 Baity 如何进行远端同步，以及会交换哪些数据。

1. Synchronization logic / 同步逻辑：
    Baity is hosted on Cloudflare Workers and uses KV as storage; the client reads remote user data from `GET /users.json` and uploads local sync configuration when it changes via `POST /report`, using `POST /register` once to obtain a per-user write token. 

    Baity 部署在 Cloudflare Workers 上并使用 KV 作为存储；客户端通过 `GET /users.json` 拉取远端用户数据，并在本地同步配置发生变化时通过 `POST /report` 上报；同时会通过一次 `POST /register` 获取每个用户对应的写入令牌。

2. User information fetched / 获取到的用户信息：
    the backend returns only what is needed for Baity presence synchronization, including users’ UUID and Baity’s synchronized configuration (e.g. SmolPeople / NickTweaks).

    后端只返回用于 Baity 远端同步所需的数据，包括用户 UUID 以及 Baity 的同步配置（例如 SmolPeople / NickTweaks）。
3. Risk acceptance / 风险承担：
    by using this mod, you agree that remote synchronization may involve privacy, network, and policy risks, and you take responsibility for any consequences at your own risk.

    使用本模组即表示你同意：远端同步可能涉及隐私、网络与合规等风险，并且你会对其产生的所有后果自担风险。

