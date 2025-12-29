import type { Config } from "@docusaurus/types"
import type * as Preset from "@docusaurus/preset-classic"

import { themes } from "prism-react-renderer"
const lightTheme = themes.github
const darkTheme = themes.dracula

const config: Config = {
  title: "Pointer Replacer",
  tagline: "Module to Replace Touch Pointer on Android",
  url: "https://pointer-replacer.web.app",
  baseUrl: "/",
  onBrokenLinks: "throw",
  onBrokenMarkdownLinks: "warn",
  favicon: "img/favicon.ico",
  organizationName: "afterroot", // Usually your GitHub org/user name.
  projectName: "pointer_replacer", // Usually your repo name.

  presets: [
    [
      "classic",
      {
        docs: {
          sidebarPath: require.resolve("./sidebars.js"),
          // Please change this to your repo.
          editUrl: "https://github.com/thesandipv/allusive-web/edit/main/docs/",
        },
        blog: {
          showReadingTime: true,
          // Please change this to your repo.
          editUrl: "https://github.com/thesandipv/allusive-web/edit/main/blog/",
        },
        theme: {
          customCss: require.resolve("./src/css/custom.css"),
        },
      } satisfies Preset.Options,
    ],
  ],

  plugins: [
    async function tailwindPlugin(context, options) {
      return {
        name: "tailwind-plugin",
        configurePostCss(postcssOptions) {
          // Appends TailwindCSS and AutoPrefixer.
          postcssOptions.plugins.push(require("tailwindcss"))
          postcssOptions.plugins.push(require("autoprefixer"))
          return postcssOptions
        },
      }
    },
  ],
  themeConfig: {
    colorMode: {
      defaultMode: "dark",
      respectPrefersColorScheme: true,
    },
    navbar: {
      title: "Pointer Replacer",
      logo: {
        alt: "Pointer Replacer Logo",
        src: "img/logo.svg",
      },
      items: [
        {
          type: "doc",
          docId: "intro",
          position: "left",
          label: "Docs",
        },
        // {to: '/blog', label: 'Blog', position: 'left'},
        {
          href: "https://github.com/thesandipv/pointer_replacer",
          // label: 'GitHub',
          "aria-label": "GitHub repository",
          className: "header-github-link",
          position: "right",
        },
      ],
    },
    footer: {
      style: "dark",
      links: [
        /*  {
            title: 'Docs',
            items: [
              {
                label: 'Intro',
                to: '/docs/intro',
              },
            ],
          }, */
        {
          title: "Community",
          items: [
            {
              label: "Telegram",
              href: "https://t.me/prdiscussion",
            },
            {
              label: "Twitter",
              href: "https://twitter.com/afterroot",
            },
          ],
        },
        {
          title: "More",
          items: [
            {
              label: "AfterROOT",
              href: "https://afterroot.web.app",
            },
            {
              label: "GitHub",
              href: "https://github.com/thesandipv/pointer_replacer",
            },
          ],
        },
      ],
      logo: {
        alt: "AfterROOT Logo",
        src: "img/afterroot.png",
        href: "https://afterroot.web.app",
      },
      copyright: `Copyright © ${new Date().getFullYear()} AfterROOT. Built with Docusaurus.`,
    },
    prism: {
      theme: lightTheme,
      darkTheme: darkTheme,
    },
  } satisfies Preset.ThemeConfig,
}

module.exports = config
