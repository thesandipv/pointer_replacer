import { themes as prismThemes } from "prism-react-renderer"
import type { Config } from "@docusaurus/types"
import type * as Preset from "@docusaurus/preset-classic"

const config: Config = {
  title: "Pointer Replacer",
  tagline: "Module to Replace Touch Pointer on Android",
  favicon: "img/favicon.ico",

  // Future flags, see https://docusaurus.io/docs/api/docusaurus-config#future
  future: {
    v4: true, // Improve compatibility with the upcoming Docusaurus v4
  },

  // Set the production url of your site here
  url: "https://pointer-replacer.web.app",
  // Set the /<baseUrl>/ pathname under which your site is served
  // For GitHub pages deployment, it is often '/<projectName>/'
  baseUrl: "/",

  // GitHub pages deployment config.
  // If you aren't using GitHub pages, you don't need these.
  organizationName: "thesandipv", // Usually your GitHub org/user name.
  projectName: "pointer_replacer", // Usually your repo name.

  onBrokenLinks: "throw",

  // Even if you don't use internationalization, you can use this field to set
  // useful metadata like html lang. For example, if your site is Chinese, you
  // may want to replace "en" with "zh-Hans".
  i18n: {
    defaultLocale: "en",
    locales: ["en"],
  },

  presets: [
    [
      "classic",
      {
        docs: {
          sidebarPath: require.resolve("./sidebars.js"),
          // Please change this to your repo.
          editUrl:
            "https://github.com/thesandipv/pointer_replacer/edit/main/website/docs/",
        },
        blog: {
          showReadingTime: true,
          feedOptions: {
            type: ["rss", "atom"],
            xslt: true,
          },
          // Please change this to your repo.
          editUrl:
            "https://github.com/thesandipv/pointer_replacer/edit/main/website/blog/",
          // Useful options to enforce blogging best practices
          onInlineTags: "warn",
          onInlineAuthors: "warn",
          onUntruncatedBlogPosts: "warn",
        },
        theme: {
          customCss: "./src/css/custom.css",
        },
      } satisfies Preset.Options,
    ],
  ],

  plugins: [
    async function tailwindPlugin(context, options) {
      return {
        name: "tailwind-plugin",
        configurePostCss(postcssOptions) {
          // Appends TailwindCSS
          postcssOptions.plugins.push(require("@tailwindcss/postcss"))
          return postcssOptions
        },
      }
    },
  ],
  themeConfig: {
    // Replace with your project's social card
    // image: "img/docusaurus-social-card.jpg",
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
              label: "X",
              href: "https://x.com/afterroot",
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
      theme: prismThemes.github,
      darkTheme: prismThemes.dracula,
    },
  } satisfies Preset.ThemeConfig,
}

module.exports = config
