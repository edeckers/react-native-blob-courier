module.exports = {
  preset: 'react-native',
  modulePathIgnorePatterns: ['<rootDir>/lib/'],
  reporters: [
    'default',
    [
      'jest-junit',
      {
        suiteName: 'jest tests',
        outputDirectory: './output',
        outputName: 'typescript-test-output.xml',
      },
    ],
  ],
};
