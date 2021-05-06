/** @type {import('@docusaurus/types').DocusaurusConfig} */
module.exports = {
  title: 'Pointer Replacer',
  tagline: 'Module to Replace Touch Pointer on Android',
  url: 'https://pointer-replacer.web.app',
  baseUrl: '/',
  onBrokenLinks: 'throw',
  onBrokenMarkdownLinks: 'warn',
  favicon: 'img/favicon.ico',
  organizationName: 'afterroot', // Usually your GitHub org/user name.
  projectName: 'pointer-replacer', // Usually your repo name.
  themeConfig: {
    navbar: {
      title: 'Pointer Replacer',
      logo: {
        alt: 'Pointer Replacer Logo',
        src: 'img/logo.svg',
      },
      items: [
        /* {
          type: 'doc',
          docId: 'intro',
          position: 'left',
          label: 'Docs',
        }, */
        // {to: '/blog', label: 'Blog', position: 'left'},
        {
          href: 'https://github.com/thesandipv/pointer-replacer',
          label: 'GitHub',
          position: 'right',
        },
      ],
    },
    footer: {
      style: 'dark',
      links: [
        /* {
          title: 'Docs',
          items: [
            {
              label: 'Tutorial',
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
              href: 'https://github.com/thesandipv/pointer-replacer',
            },
          ],
        },
      ],
      copyright: `Copyright © ${new Date().getFullYear()} AfterROOT. Built with Docusaurus.`,
    },
  },
  presets: [
    [
      '@docusaurus/preset-classic',
      {
        docs: {
          sidebarPath: require.resolve('./sidebars.js'),
          // Please change this to your repo.
          editUrl:
            'https://github.com/thesandipv/pointer-replacer/edit/master/web/',
        },
        blog: {
          showReadingTime: true,
          // Please change this to your repo.
          editUrl:
            'https://github.com/thesandipv/pointer-replacer/edit/master/web/blog/',
        },
        theme: {
          customCss: require.resolve('./src/css/custom.css'),
        },
      },
    ],
  ],
};
