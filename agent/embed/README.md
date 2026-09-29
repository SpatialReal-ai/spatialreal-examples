# Agent in an iframe

Talk to a [SpatialReal Agent](https://docs.spatialreal.ai/agent/introduction) on a page of your own. The only code is one `<iframe>`.

Docs: [Quickstart](https://docs.spatialreal.ai/overview/quickstart)

## You need

- A [SpatialReal Studio](https://app.spatialreal.ai) account
- A browser with a microphone

## Run it

1. In Studio, create an agent — describe it in a sentence and let Studio build it.
2. On the agent, add a **Website embed** link and set it to **Published**.
3. Copy the link's ID into `index.html`, in place of `YOUR_LINK_ID`.
4. Open `index.html` in your browser, click **Start**, allow the microphone and speak.

Keep `allow="microphone; autoplay"` on the iframe: without it the agent can't hear you.

Before you put the page online, add its address to the link's **Allowed sites** in Studio.
