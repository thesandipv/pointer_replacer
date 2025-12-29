import useDocusaurusContext from "@docusaurus/useDocusaurusContext"
import Layout from "@theme/Layout"
import clsx from "clsx"
import React from "react"
import styles from "./index.module.css"

function HomepageHeader() {
  const { siteConfig } = useDocusaurusContext()
  return (
    <header className={clsx("hero hero--primary", styles.heroBanner)}>
      <div className="container">
        <h1 className="hero__title">{siteConfig.title}</h1>
        <p className="hero__subtitle">{siteConfig.tagline}</p>
      </div>
    </header>
  )
}

export default function Home() {
  const { siteConfig } = useDocusaurusContext()
  return (
    <Layout description={siteConfig.tagline}>
      <HomepageHeader />
      <main>
        <div className="container video-container my-2">
          <iframe
            src="https://www.youtube.com/embed/eWcV0PUwlzs"
            frameBorder="0"
            allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture"
            allowFullScreen
          />
        </div>

        <div className="flex flex-col items-center my-2">
          <a href="https://play.google.com/store/apps/details?id=com.afterroot.allusive2&utm_source=pr-web&pcampaignid=pcampaignidMKT-Other-global-all-co-prtnr-py-PartBadge-Mar2515-1">
            <img
              className="max-w-sm"
              alt="Get it on Google Play"
              src="https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png"
            />
          </a>
          <p className="text-sm">
            Google Play and the Google Play logo are trademarks of Google LLC.
          </p>
        </div>
      </main>
    </Layout>
  )
}
