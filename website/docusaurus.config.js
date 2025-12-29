// Note: type annotations allow type checking and IDEs autocompletion

const lightCodeTheme = require('prism-react-renderer/themes/github');
const darkCodeTheme = require('prism-react-renderer/themes/dracula');

/** @type {import('@docusaurus/types').DocusaurusConfig} */
const config = {
  title: 'Pointer Replacer',
  tagline: 'Module to Replace Touch Pointer on Android',
  url: 'https://pointer-replacer.web.app',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: 'afterroot', // Usually your GitHub org/user name.
  projectName: 'pointer_replacer', // Usually your repo name.

  presets: [
    [
      'classic',
      /** @type {import('@docusaurus/preset-classic').Options} */
      ({
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          // Please change this to your repo.
          editUrl: 'https://github.com/thesandipv/allusive-web/edit/main/docs/',
        },
        blog: {
          showReadingTime: true,
          // Please change this to your repo.
          editUrl: 'https://github.com/thesandipv/allusive-web/edit/main/blog/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      }),
    ],
  ],

  plugins: [
    async function tailwindPlugin(context, options) {
      return {
        name: 'tailwind-plugin',
        configurePostCss(postcssOptions) {
          // Appends TailwindCSS and AutoPrefixer.
          postcssOptions.plugins.push(require('tailwindcss'));
          postcssOptions.plugins.push(require('autoprefixer'));
          return postcssOptions;
        },
      };
    },
  ],
  themeConfig:
    /** @type {import('@docusaurus/preset-classic').ThemeConfig} */
    ({
      colorMode: {
        defaultMode: 'dark',
        respectPrefersColorScheme: true,
      },
      navbar: {
        title: 'Pointer Replacer',
        logo: {
          alt: 'Pointer Replacer Logo',
          src: 'img/logo.svg',
        },
        items: [
          {
            type: 'doc',
            docId: 'intro',
            position: 'left',
            label: 'Docs',
          },
          // {to: '/blog', label: 'Blog', position: 'left'},
          {
            href: 'https://github.com/thesandipv/pointer_replacer',
            // label: 'GitHub',
            'aria-label': 'GitHub repository',
            className: 'header-github-link',
            position: 'right',
          },
        ],
      },
      footer: {
        style: 'dark',
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
            title: 'Community',
            items: [
              {
                label: 'Telegram',
                href: 'https://t.me/prdiscussion',
              },
              {
                label: 'Twitter',
                href: 'https://twitter.com/afterroot',
              },
            ],
          },
          {
            title: 'More',
            items: [
              {
                label: 'AfterROOT',
                href: 'https://afterroot.web.app',
              },
              {
                label: 'GitHub',
                href: 'https://github.com/thesandipv/pointer_replacer',
              },
            ],
          },
        ],
        logo: {
          alt: 'AfterROOT Logo',
          src: 'img/afterroot.png',
          href: 'https://afterroot.web.app',
        },
        copyright: `Copyright © ${new Date().getFullYear()} AfterROOT. Built with Docusaurus.`,
      },
      prism: {
        theme: lightCodeTheme,
        darkTheme: darkCodeTheme,
      },
    }),
};

module.exports = config;
